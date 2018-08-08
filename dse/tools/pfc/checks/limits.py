import logging
import os

limits_file = '/etc/security/limits.conf'
suggested_limits = '''
    * soft nofile 32768
    * hard nofile 32768
    root soft nofile 32768
    root hard nofile 32768
    * soft memlock unlimited
    * hard memlock unlimited
    root soft memlock unlimited
    root hard memlock unlimited
    * soft as unlimited
    * hard as unlimited
    root soft as unlimited
    root hard as unlimited
'''.strip().split('\n')
suggested_limits = [limit.strip() for limit in suggested_limits]

def check(options):
    # If the file DNE, create the file from scratch
    if not os.path.isfile(limits_file):
        if options.fix:
            logging.info('%s not found. Attempting to create file...' % limits_file)
            return fix(suggested_limits)
        else:
            logging.warn('%s not found.' % limits_file)
            return False

    if not os.access(limits_file, os.R_OK):
        logging.error('Permission to read file %s denied. Please check permissions or run as sudo.' % limits_file)
        return False

    # Find missing limits
    try:
        f = open(limits_file)
        limits = f.read()
    finally:
        f.close()

    # Collect missing links
    missing_limits = []
    for limit in suggested_limits:
        if not limit in limits:
            missing_limits.append(limit)

    # Append missing links
    if missing_limits:
        if options.fix:
            logging.warn('%s missing limits found. Attempting to add limits...' % len(missing_limits))
            return fix(missing_limits)
        else:
            logging.error('%s missing limits found:' % len(missing_limits))
            for limit in missing_limits:
                logging.error('\t%s' % limit)
            return False

    logging.info('Limits were correctly setup.')
    return True

def fix(missing_limits):
    # Ensure directory that houses limits.conf exists and is writable
    limits_directory = os.path.dirname(limits_file)
    if not os.access(limits_directory, os.F_OK) or not os.access(limits_directory, os.W_OK):
        logging.error('Permission to write file %s to %s denied. Please check permissions or run as sudo.' %  (
                          os.path.basename(limits_file),
                          limits_directory))
        return False

    # Append missing limits
    try:
        f = open(limits_file, 'a')
        for limit in missing_limits:
            logging.info('Adding limit: %s' % limit.strip())
            f.write(limit.strip() + '\n')
    finally:
        f.close()

    logging.info('Limits were fixed.')
    return True

