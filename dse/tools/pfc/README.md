# Usage

Run `sudo ./preflight_check` to run all checks.

# Create New Test

* Create a new Python file in `checks/`.
* Add module to `__all__` in `checks/__init__.py`.
* Add `<module>.check()` in `preflight_check`.
* Try to keep all checks minimal with only check() and fix() methods.
* You can use `command_line.execute()` to get back a Response which contains:
    * `command`
    * `stdout`
    * `stderr`
    * `report_failure()`
