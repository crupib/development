import logging
import os
import platform

nproc_file = '/etc/security/limits.d/90-nproc.conf'
ideal_value = 10240
ideal_statement = '*  soft  nproc  %s' % ideal_value

def check(options):
    '''
    A specific check for only CentOS, RHEL, or OEL Systems.

    For more information, see:
    https://bugzilla.redhat.com/show_bug.cgi?id=919793
    '''

    # Ignore cases and OSs where nproc_file is not set
    if not os.path.isfile(nproc_file):
        logging.info('\'%s\' does not exist. Shouldn\'t need this fix.' % nproc_file)
        return True

    if not os.access(nproc_file, os.R_OK):
        logging.error('Permission to read file %s denied. Please check permissions or run as sudo.' % nproc_file)
        return False

    try:
        f = open(nproc_file)
        file_contents = f.read().split('\n')
    finally:
        f.close()

    for i, line in enumerate(file_contents):
        # Search file for nproc definitions
        if 'nproc' in line:
            line_contents = line.split()

            # Specifically look for ideal_statement
            if line_contents[0] == '*' and line_contents[1] == 'soft' and \
                                           line_contents[2] == 'nproc' and \
                                           int(line_contents[3]) < ideal_value:

                # Patch the file, if it's an expected OS
                this_os = platform.linux_distribution()[0]
                if options.fix and this_os.lower() in ['centos', 'redhat', 'oel']:
                    return fix(file_contents, i, line)

                # Else, only warn for manual correction.
                else:
                    logging.warn('"%s" should be set to "%s" in \'%s\' if on CentOS, RHEL, or OEL Systems.' % (
                        line.strip(), ideal_statement, nproc_file))
                    return False

    logging.info('Nproc properly set.')
    return True

def fix(file_contents, i, line):
    # Ensure conf is writable
    if not os.access(nproc_file, os.F_OK) or not os.access(nproc_file, os.W_OK):
        logging.error('Permission to write to file %s denied.' %  (nproc_file))
        return False

    line_contents = line.split()
    file_contents[i] = line.replace(line_contents[3], str(ideal_value))

    try:
        f = open(nproc_file, 'w')
        f.write('\n'.join(file_contents))
    finally:
        f.close()

    logging.info('Replaced `%s` with `%s` in %s.' % (' '.join(line.split()),
                                                     ' '.join(file_contents[i].split()),
                                                     nproc_file))
    logging.warn('Please start a new shell session.')
    return True
