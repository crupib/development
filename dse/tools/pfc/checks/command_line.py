import logging
import shlex
import subprocess

class Response:
    def report_failure(self):
        logging.error('Failed to execute `%s`: "%s"' % (self.command, self.stderr))
        return False

def execute(command):
    process = subprocess.Popen(shlex.split(command), stderr=subprocess.PIPE, stdout=subprocess.PIPE)
    read = process.communicate()

    response = Response()
    response.command = command.strip()
    response.stdout = read[0].strip()
    response.stderr = read[1].strip()

    return response
