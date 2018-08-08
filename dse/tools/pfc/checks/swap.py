import logging
import platform

import command_line

check_command = 'free -m'
set_command = 'sudo swapoff --all'

def check(options):
    # Ignore on OSX
    if platform.system() in ['Darwin']:
        logging.info('Disabling swap not available on %s.' % (platform.system()))
        return True

    # Check on the current swap values
    response = command_line.execute(check_command)
    if response.stderr:
        return response.report_failure()

    found_expected_output = False
    for line in response.stdout.split('\n'):
        # Look for line with swap information
        if line.split()[0] == 'Swap:':
            found_expected_output = True

            # Ensure that the total swap is 0
            if len(line.split()) > 1 and line.split()[1].isdigit() and int(line.split()[1]) > 0:
                if options.fix:
                    return fix()
                else:
                    logging.error('Swap not disabled.')
                    return False

    if not found_expected_output:
        logging.warn('Did not find expected output for `%s`.' % check_command)
        return False

    logging.info('Swap disabled.')
    return True

def fix():
    response = command_line.execute(set_command)
    if response.stderr:
        return response.report_failure()

    logging.info('Disabled swap.')
    return True
