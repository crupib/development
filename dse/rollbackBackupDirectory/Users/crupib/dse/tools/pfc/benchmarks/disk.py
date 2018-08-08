import logging
import threading

from iops import *

disk_duration = 5
disk_threads = 32

def iops_test(dev):
    '''Test runner modeled closely after iops.__main__()'''

    blocksize = 512
    try:
        print "%s, %sB, %d threads, %s seconds per test:" % (dev, greek(mediasize(dev), 2, 'si'), disk_threads, disk_duration)
        _iops = disk_threads+1 # initial loop
        while _iops > disk_threads and blocksize < mediasize(dev):
            # threading boilerplate
            threads = []
            results = []

            def results_wrap(results, func, *__args, **__kw):
                """collect return values from func"""
                result = func(*__args, **__kw)
                results.append(result)

            for i in range(0, disk_threads):
                _t = threading.Thread(target=results_wrap, args=(results, iops, dev, blocksize, disk_duration,))
                _t.start()
                threads.append(_t)

            for _t in threads:
                _t.join()
            _iops = sum(results)

            bandwidth = int(blocksize*_iops)
            print " %sB blocks: %6.1f IO/s, %sB/s (%sbit/s)" % (greek(blocksize), _iops,
                greek(bandwidth, 1), greek(8*bandwidth, 1, 'si'))

            blocksize *= 2
    except IOError, (err_no, err_str):
        logging.error('%s: %s' % (dev, err_str))
        return False
    except OSError, (err_no, err_str):
        logging.error('%s: %s' % (dev, err_str))
        return False

    return True


def check(options):
    if not options.devices:
        logging.warn('Use --device to signal devices to benchmark.')
        return False

    if options.disk_duration:
        globals()['disk_duration'] = options.disk_duration

    if options.disk_threads:
        globals()['disk_threads'] = options.disk_threads

    devices = options.devices.split(',')

    failure = False
    for dev in devices:
        try:
            # Benchmark each device
            failure = not iops_test(dev) or failure
        except KeyboardInterrupt:
            print
            logging.warn('Benchmark cancelled.')
            return False
        print

    if failure:
        logging.error('Encountered error while running disk benchmarking tests.')
        return False

    logging.info('Disk benchmarking completed successfully.')
    return True

def fix():
    '''Not implemented. Report only module.'''
    pass
