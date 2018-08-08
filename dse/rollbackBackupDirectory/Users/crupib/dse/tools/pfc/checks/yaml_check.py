import logging
import os

try:
    import yaml
except:
    yaml = None

default_locations = [
    '/etc/cassandra',
    '/etc/dse/cassandra',
    'conf',
    'resources/cassandra/conf',
]

def check(options):
    if not yaml:
        logging.error('`yaml` module must be installed in order to run this check.')
        return False

    if not options.yaml_location:
        found = False

        # Search known default cassandra.yaml locations
        for yaml_location in default_locations:
            if os.access(yaml_location, os.R_OK):
                found = True
                break

        if not found:
            logging.error('Searched for cassandra.yaml in the following locations:')
            logging.error('\t%s' % default_locations)
            logging.error('Use --yaml to specify cassandra.yaml location or check permissions.')
            return False
    else:
        yaml_location = options.yaml_location

    # Ensure we're pointing to the actual file, not just the directory
    if not os.path.basename(yaml_location) == 'cassandra.yaml':
        yaml_location = os.path.join(yaml_location, 'cassandra.yaml')

    # Confirm we can read the actual file
    if not os.access(yaml_location, os.R_OK):
        logging.error('Permission to read file %s denied. Please check permissions or run as sudo.' % yaml_location)
        return False

    try:
        f = open(yaml_location)
        cassandra_yaml = yaml.load(f)
    finally:
        f.close()

    # Ensure each data directory is separate from the commitlog directory
    for data_directory in cassandra_yaml['data_file_directories']:
        if os.path.dirname(data_directory) == os.path.dirname(cassandra_yaml['commitlog_directory']):
            logging.warn('Please ensure \'%s\' and \'%s\' are linked to separate devices.' % (data_directory, cassandra_yaml['commitlog_directory']))
            return False

    logging.info('Yaml checks completed successfully.')
    return True

def fix():
    '''Not implemented. Report only module.'''
    pass
