#!/bin/bash
#
# Script to manually install datastax-agent dependencies and
# datastax-agent itself.

usage() {
    cat <<EOF
    Usage: $0 [OPTIONS] <opscenterd address>

    OPTIONS:
        -s  Enable SSL. [Default: disabled]
        -h  Displays this help message

EOF
}

echoerr() { echo "$@" 1>&2; }

SSL_ENABLED=0
SSL_STRING=""
while getopts "hs" OPTION
do
     case $OPTION in
         h)
             usage
             exit 1
             ;;
         s)
             SSL_STRING="-s"
             SSL_ENABLED=1
             ;;
         ?)
             usage
             exit
             ;;
     esac
done
shift $(( ${OPTIND} - 1 ));

if [ $# -lt 1 ]; then
    usage
    exit 2
fi

NAME='datastax-agent'
USER='cassandra'
OPSCENTER_ADDR=$1
ADDR_CONF="/var/lib/$NAME/conf/address.yaml"
ADDR_CONF_BACKUP="$ADDR_CONF.bak"
CERT_DIR="/var/lib/$NAME/ssl"
LOG="/var/log/$NAME/installer.log"
AMI_CONF="$HOME/datastax_ami/ami.conf"
BIN_DIR="$(dirname $(readlink -e "$0"))"
BASE_DIR="$(dirname "$BIN_DIR")"

# Deb-based systems can have yum present (usually on dev systems to handle
# foreign packages). Verify that we're rpm-based by checking release-files.
#   - /etc/redhat-release is present on RHEL, CentOS, and Oracle Linux
#   - /etc/SuSe-release is present on SuSe systems
# The methods used to detect the package manager should be kept in sync
# with the _getPackageManager method in Agents.py
if [ -x "$(which yum)" ] && \
   [ -f "/etc/redhat-release" -o -f "/etc/SuSE-release" ]; then
    PM="yum"
    SERVICE="/sbin/service"
    RMPKG="remove"
    AGENT_PKG="$BASE_DIR/datastax-agent.rpm"
    pkgver=$(rpm -q --qf '%{VERSION}' -p "$AGENT_PKG" 2>/dev/null)
    if rpm -q $NAME; then
        installedver=$(rpm -q --qf '%{VERSION}' "$NAME")
    else
        installedver="-1"
    fi


    INSTALLED_VERSION=$(echo $installedver | sed -e 's/SNAPSHOT//')
    REQUESTED_VERSION=$(echo $pkgver | sed -e 's/SNAPSHOT//')

    IFS=. read -a installed <<< "${INSTALLED_VERSION}"
    IFS=. read -a requested <<< "${REQUESTED_VERSION}"
    downgrade=1
    prev_equal=1
    for ((i=0; i < ${#installed[@]}; i++)); do
        if [[ $i -eq 0 && ${installed[$i]} -gt ${requested[$i]} ]]; then
            break
        elif [[ ${installed[$i]} -eq ${requested[$i]} ]]; then
            prev_equal=1
        elif [[ ${prev_equal} -eq 1 && ${installed[$i]} -lt ${requested[$i]} ]]; then
           downgrade=0
           break
        else
            prev_equal=0
        fi
    done
    if [ "$installedver" == "$pkgver" ]; then
        INSTALL="reinstall"
    elif [[ ( $downgrade -eq 1 ) ]] ;then
        INSTALL="downgrade"
    else
        INSTALL="install"
    fi

    INSTALLCMD="$PM --disablerepo=* -y $INSTALL $AGENT_PKG --nogpgcheck"
elif [ -x "$(which apt-get)" ]; then
    PM="apt-get"
    SERVICE="service"
    RMPKG="purge"
    AGENT_PKG="$BASE_DIR/datastax-agent.deb"
    pkgver=$(dpkg-deb -f "$AGENT_PKG" Version)
    INSTALLCMD="dpkg -i $AGENT_PKG"
else
    echoerr "Unable to find a package manager."
    exit 3
fi

JAVA=`$BIN_DIR/find-java`
if [ $? -ne 0 ]; then
    echoerr "Unable to find a java executable!"
    echoerr "Please install java or set JAVA_HOME."
    exit 4
fi

DO_SUDO=sudo

if [ "$(id -u)" -ne 0 ]; then
    SUDOARGS="-E "
    pfile="$BASE_DIR/pfile"
    if [ -r "$pfile" ]; then
        chmod 700 "$pfile"
        export SUDO_ASKPASS="$pfile"
        SUDOARGS="$SUDOARGS -A"
    fi
    wrapper="$BIN_DIR/sudowrap"
    [ -x "$wrapper" ] && DO_SUDO="$wrapper"
    if [ "$SSL_ENABLED" == "1" ]; then
        exec "$DO_SUDO" $SUDOARGS "$0" "$SSL_STRING" "$@"
    else
        exec "$DO_SUDO" $SUDOARGS "$0" "$@"
    fi
    echoerr "Not root, can't sudo. Failing..." >&2
    exit 1
fi

mkdir -p "$(dirname "$LOG")"

log () {
    stamp=$(date +'%F %T %z')
    "$@" 2>&1 | while read line; do
        echo "$stamp  $line" >> "$LOG"
        echo "$line"
    done
    return ${PIPESTATUS[0]}
}

lecho () {
    log echo "$*"
}

die () {
    lecho "FAILURE: $*"
    echoerr "$*"
    exit 1
}

if [ -e $AMI_CONF ]; then
    have_java="no"
    for i in {1..60}; do
        out="$(grep "currentstatus = Complete\!" $AMI_CONF 2>&1)"
        if [ -z "$out" ]; then
            lecho "Sleeping for 5s to wait for AMI installation to be completed"
            sleep 5
        else
            have_java="yes"
            source ~/.profile
            break
        fi
    done

    if [ "$have_java" == "no" ]; then
        die "Timed out waiting to finish installing DataStax AMI"
    else
        lecho "AMI successfully installed"
    fi
else
    lecho "DataStax AMI wasn't used"
fi

# Try to make sure we can obtain the dpkg lock. If it appears to be held
# by another process, sleep and retry for 60s.
have_lock="no"
if [ "$PM" == "apt-get" ]; then
    #Debian Wheezy doesn't have fuser installed
    FUSER_EXISTS="`which fuser`"
    if [ "$FUSER_EXISTS" == "" ]; then
        log $PM -qy install psmisc
        FUSER_RET=$?
        if [[ $FUSER_RET -ne 0 ]]; then
            die "Could not install psmisc, which is needed for fuser."
        fi
    fi

    for i in {1..12}; do
        out="$(fuser /var/lib/dpkg/lock 2>&1)"
        if [ -n "$out" ]; then
            lecho "Sleeping for 5s to wait for dpkg lock to be released"
            sleep 5
        else
            have_lock="yes"
            break
        fi
    done
    if [ "$have_lock" == "no" ]; then
        die "Timed out waiting to obtain the dpkg lock, which is held by $(fuser -u /var/lib/dpkg/lock 2>&1)"
    fi
fi

log $SERVICE $NAME stop
log $PM $RMPKG -y opscenter-agent

lecho "Starting agent installation process for version $pkgver"

log $PM -qy install sysstat || \
    die 'The package "sysstat" does not appear to be available or installable.'

log $INSTALLCMD || \
    die "Unable to install the datastax-agent package. Please check your $PM configuration as well as the agent install log ($LOG)."

lecho "Installing certificates from opscenterd..."
# put key and certs in place
mkdir -p $CERT_DIR
if [ -f "$BASE_DIR/agentKeyStore" ]; then
    # remote auto install
    log cp -v "$BASE_DIR/agentKeyStore" "$CERT_DIR"
else
    # manual install
    log cp -v "ssl/agentKeyStore" "$CERT_DIR"
fi
lecho "Setting up agent node state..."

# Set up the config file that holds opscenterd and local addresses
log mkdir -pv "$(dirname "$ADDR_CONF")"

USE_SSL=0
if [ "$SSL_ENABLED" == "1" ]; then
    lecho "Enabling SSL"
    USE_SSL=1
fi

replace_in_yaml() {
    local key=$2 value=$3
    local s='[[:space:]]*' tmp="/tmp/addr_conf"
    if grep -q "$s$key$s:" $1; then
        lecho "Replacing $key with $value"
        sed -E "s/$s$key$s:$s.*/$key: $value/" "$1" > "$tmp"
        mv "$tmp" "$1"
    else
        lecho "Adding to config: $key: $value"
        echo "$key: $value" >> $1
    fi
}

# Do not touch any other existing settings
lecho "Setting up agent config..."
if [[ -e "$ADDR_CONF" ]]; then
    lecho "Backup up existing agent config..."
    log cp "$ADDR_CONF" "$ADDR_CONF_BACKUP"
else
    log touch "$ADDR_CONF"
fi

replace_in_yaml "$ADDR_CONF" "stomp_interface" "$OPSCENTER_ADDR"
replace_in_yaml "$ADDR_CONF" "use_ssl" $USE_SSL

chown -RL $USER:$USER "/var/lib/$NAME" "/var/log/$NAME"

lecho "Starting new agent..."

$SERVICE "$NAME" status &> /dev/null
if [ "$?" -eq "0" ]; then
    log $SERVICE "$NAME" restart
else
    log $SERVICE "$NAME" start
fi

lecho "Agent installation complete."
