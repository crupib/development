import logging
import os
import platform

fstab_file = '/etc/fstab'

# noatime will track when inodes were last accessed. introduces SSD wear.
# nodiratime will track when directory inodes were last accessed. introduces SSD wear.
# Ext4 supports TRIM. maintains performance. tracks blocks that are safely cleared.
ideal_settings = ['discard']

# read queues are given a higher priority, because processes usually block on read operations.
ideal_scheduler = 'deadline'

def check(options):
    if platform.system() in ['Darwin']:
        logging.info('Fstab settings not available on %s.' % platform.system())
        return True

    # WARN if ssd option is not set
    if options.ssd == None:
        logging.warn('Use --ssd or --nossd to signal ssd usage.')
        return False

    # Ignore check if --nossd option is set
    if not options.ssd:
        logging.info('SSD check skipped.')
        return True

    if not os.access(fstab_file, os.R_OK):
        logging.error('Permission to read file %s denied. Please check permissions or run as sudo.' % fstab_file)
        return False

    try:
        f = open(fstab_file)
        fstab = f.read().split('\n')
    finally:
        f.close()

    found_devices = []
    for line in fstab:
        if len(line.split()) > 3:
            device = line.split()[0]

            if not device in options.ssd:
                continue
            found_devices.append(device)

            settings = line.split()[3]

            for setting in ideal_settings:
                if not setting in settings:
                    logging.error('%s: "%s" not found in %s' % (device, setting, fstab_file))

            # Check scheduler
            scheduler_file = os.path.join('/', 'sys', 'block', os.path.basename(device), 'queue', 'scheduler')
            if not os.access(scheduler_file, os.R_OK):
                logging.error('Permission to read file %s denied. Please check permissions or run as sudo.' % scheduler_file)
            else:
                try:
                    f = open(scheduler_file)
                    scheduler = f.read()
                finally:
                    f.close()

                # Ensure ideal_scheduler is the default scheduler
                if not ('[%s]' % ideal_scheduler) in scheduler:
                    logging.error('Execute the following as root, if SSD: `echo %s > %s`' % (ideal_scheduler, scheduler_file))

        elif len(line.strip()) > 0:
            logging.warn('Unexpected fstab line: %s' % line)

    logging.info('Reported incorrect settings for devices: %s' % found_devices)
    return True

def fix():
    '''Not implemented. Report only module.'''
    pass
