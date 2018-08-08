import ConfigParser
import sys

from cassandra.auth import SaslAuthProvider

def get_auth_provider(config_file, env):
    configs = ConfigParser.SafeConfigParser()
    configs.read(config_file)

    def get_option(section, option):
        try:
            return configs.get(section, option)
        except ConfigParser.Error:
            return None

    def exit_if_none(section_name, option_name, env_name, value):
        if value is None:
            sys.exit("%s authentication requires '%s' option in [%s] section in %s or %s environment variable "
                     "to be specified." % (section_name.title(), option_name, section_name, config_file, env_name))

    """
    Kerberos auth provider can be configured in the cqlshrc file [kerberos] section
    or with environment variables:

    Config option      Environment Variable      Description
    -------------      --------------------      -----------
    hostname           KRB_HOSTNAME              Hostname to authenticate against
    service            KRB_SERVICE               Service to authenticate with
    principal          KRB_PRINCIPAL             Principal name to authenticate (default: None)
    qops               QOPS                      Comma separated list of QOP values (default: auth)
    """
    if configs.has_section('kerberos') or \
        (env.get('KRB_HOSTNAME') is not None and env.get('KRB_SERVICE') is not None):
        krb_host = env.get('KRB_HOSTNAME')
        if krb_host is None:
            krb_host = get_option('kerberos', 'hostname')
        exit_if_none('kerberos', 'hostname', 'KRB_HOSTNAME', krb_host)

        krb_service = env.get('KRB_SERVICE')
        if krb_service is None:
            krb_service = get_option('kerberos', 'service')
        exit_if_none('kerberos', 'service', 'KRB_SERVICE', krb_service)

        krb_principal = env.get('KRB_PRINCIPAL')
        if krb_principal is None:
            krb_principal = get_option('kerberos', 'principal')

        krb_qop_value = env.get('QOPS')
        if krb_qop_value is None:
            krb_qop_value = get_option('kerberos', 'qops')
        if krb_qop_value is None:
            krb_qop_value = 'auth';
        krb_qops = krb_qop_value.split(',');

        sasl_kwargs = {'service': krb_service,
                       'principal': krb_principal,
                       'mechanism': 'GSSAPI',
                       'qops': krb_qops}

        return SaslAuthProvider(**sasl_kwargs)

    return None
