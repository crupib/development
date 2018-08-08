import os
import sys
import yaml

termcolor_message = False

def parse_yamls(orig_yaml_location, fresh_yaml_location):
    """Return Python dicts that resemble the yaml settings."""

    orig_yaml = yaml.load(file(os.path.expanduser(orig_yaml_location), 'r'))
    fresh_yaml = yaml.load(file(os.path.expanduser(fresh_yaml_location), 'r'))

    return fresh_yaml, orig_yaml


def read_yamls(orig_yaml_location, fresh_yaml_location):
    """Return text versions of the yamls. Useful when finding setting order."""

    orig_text = open(os.path.expanduser(orig_yaml_location)).read()
    fresh_text = open(os.path.expanduser(fresh_yaml_location)).read()

    return fresh_text, orig_text


def analyze_differences(fresh_yaml, orig_yaml, fresh_text, orig_text):
    """
    Find all differences that exist between a freshly installed cassandra.yaml
    and an existing cassandra.yaml.
    
    Add differences to a dictionary where the key is the line number where the
    setting can be found. This will be used later to sort the differences by
    how they are found in the yaml, for easier editing.
    """

    differences = {
        'changes': {},
        'new_settings': {},
        'missing_settings': {}
    }

    # iterate over all fresh keys
    for fresh_key in fresh_yaml:
        # check if fresh key existed in original yaml
        if fresh_key in orig_yaml:
            # check if difference between fresh and original values exist
            if orig_yaml[fresh_key] != fresh_yaml[fresh_key]:
                differences['changes'][fresh_text.find(fresh_key)] = {
                    'key': fresh_key,
                    'values': {
                        'orig': orig_yaml[fresh_key],
                        'fresh': fresh_yaml[fresh_key]
                    }
                }
        else:
            # separate new settings from changes
            differences['new_settings'][fresh_text.find(fresh_key)] = {
                'key': fresh_key,
                'value': fresh_yaml[fresh_key]
            }

    # iterate over original keys
    for orig_key in orig_yaml:
        # check if original key is missing from fresh yaml
        if not orig_key in fresh_yaml:
            differences['missing_settings'][orig_text.find(orig_key)] = {
                'key': orig_key,
                'value': orig_yaml[orig_key]
            }

    return differences


def special_print(text='', style='normal'):
    # check if in terminal mode, or output is being piped elsewhere
    if not hasattr(sys.stdout, "isatty") or not sys.stdout.isatty():
        # revert to standard printing
        print text

    else:
        # attempt to use termcolor to color print to the terminal
        try:
            from termcolor import cprint

            if style == 'normal':
                print text
            elif style == 'header':
                cprint(text, 'grey', 'on_white')
            elif style == 'addition':
                cprint(text, 'green')
            elif style == 'subtraction':
                cprint(text, 'red')
            elif style == 'ip_address':
                cprint(text, attrs=['bold'], end=': ')

        except ImportError:
            # catch exceptions when termcolor is not found

            # print helpful message on how to get colored output, only once
            global termcolor_message
            if not termcolor_message:
                print 'For colored output, install termcolor by running: pip install termcolor'
                print
                termcolor_message = True

            # revert to standard printing
            print text
