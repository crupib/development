import logging
import platform

import command_line

suggested_readahead = 128
reasonable_readahead = 1024
check_command = 'sudo blockdev --report'

def check(options):
    if platform.system() in ['Darwin']:
        logging.info('Blockdev settings not available on %s.' % platform.system())
        return True

    response = command_line.execute(check_command)
    if response.stderr:
        return response.report_failure()

    ensured_column = False
    failure = False
    for line in response.stdout.split('\n'):
        if len(line.split()) > 1:
            second_column = line.split()[1]
        else:
            second_column = line

        # Ensure RA is the correct column
        if not ensured_column and second_column == 'RA':
            ensured_column = True
            continue

        if ensured_column and second_column.isdigit():
            # Check if readahead needs to be fixed
            if int(second_column) > reasonable_readahead:
                device = line.split()[-1]
                if options.fix:
                    success = fix(device, second_column)
                    if not success:
                        failure = True
                else:
                    logging.warn('Readahead is set to %s for %s.' % (second_column, device))
                    logging.warn('Reasonable limit is %s.' % suggested_readahead)
                    logging.warn('Suggested value is %s.' % reasonable_readahead)
        else:
            # If ensured_column is in the first row, or
            # a later row is NaN, start failing
            ensured_column = False
            break

    if not ensured_column:
        logging.error('Output for `%s` does not match what was expected.' % response.command)
        return False

    if failure:
        logging.warn('Issues may have been encountered.')
        return False
    else:
        logging.info('Blockdev readahead values are within reason.')
        return True

def fix(device, old_readahead):
    response = command_line.execute('sudo blockdev --setra 512 %s' % device)
    if response.stderr:
        return response.report_failure()

    logging.info('Readahead for %s changed from %s to %s.' % (device, old_readahead, suggested_readahead))
    return True
