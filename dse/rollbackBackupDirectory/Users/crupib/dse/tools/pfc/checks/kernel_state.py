import logging
import os
import platform

import command_line

read_command = 'sysctl vm.max_map_count'
map_count_value = 131072
set_command = 'sysctl -w vm.max_map_count=%s' % map_count_value
sysctl_file = '/etc/sysctl.conf'

def check(options):
    # Ignore on OSX
    if platform.system() in ['Darwin']:
        logging.info('`sysctl` not available on %s.' % (platform.system()))
        return True

    # Attempt to read vm.max_map_count
    response = command_line.execute(read_command)

    # If stderr, or vm.max_map_count is not ideal, fix()
    if response.stderr:
        return response.report_failure()
    else:
        current_count_value = int(response.stdout.split()[-1])
        if current_count_value != map_count_value:
            if options.fix:
                return fix(current_count_value)
            else:
                logging.error('vm.max_map_count is set to %s.' % current_count_value)
                logging.error('Suggested value is %s.' % map_count_value)
                return False

    logging.info('vm.max_map_count set correctly.')
    return True

def fix(current_count_value=None):
    # Ensure directory that houses sysctl.conf exists and is writable
    sysctl_directory = os.path.dirname(sysctl_file)
    if not os.access(sysctl_directory, os.F_OK) or not os.access(sysctl_directory, os.W_OK):
        logging.error('Permission to write file %s to %s denied. Please check permissions or run as sudo.' % (
            os.path.basename(sysctl_file),
            sysctl_directory))
        return False

    # Append persistent setting
    try:
        f = open(sysctl_file, 'a')
        f.write('vm.max_map_count = %s\n' % map_count_value)
    finally:
        f.close()

    # Attempt to set vm.max_map_count
    response = command_line.execute(set_command)

    # If attempt failed
    if response.stderr:
        return response.report_failure()

    logging.info('Set vm.max_map_count to %s from %s.' % (map_count_value, current_count_value))
    return True
