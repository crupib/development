# Setup

PyYAML is required and can be installed via:

    pip install pyyaml

For colored output, run `pip install termcolor` before use.

# yaml_diff

Run `./yaml_diff <cassandra.yaml.orig> <cassandra.yaml.fresh>` to check differences between yaml files.

Do note that the `Missing Settings` portion will report actual missing settings as well as deprecated settings.

# cluster_check

Run `./cluster_check </path/to/cassandra.yaml> [/path/to/nodelist]` to check the differences between each node's
yaml.

The `nodelist` parameter is optional since the script will revert to checking `nodetool status` for the list of IP
addresses. The format for the nodelist file should be one address per line.

NOTE: Password-less ssh access from the current node to all other nodes is required for smooth usage.
