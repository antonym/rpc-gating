import logging
import subprocess
import traceback

import click

import ghutils
from notifications import mail

logger = logging.getLogger("release")


# release notes
@click.command()
@click.option(
    "--script",
    help="Execute this script to generate release notes."
)
@click.option(
    "--file", "rnfile",
    help="Read release notes from this file."
)
@click.option(
    "--text",
    help="Text of the release notes."
)
def generate_release_notes(script, rnfile, text):
    release_notes = ""
    if script:
        logger.debug("Generating release notes from script: {}".format(script))
        try:
            out, err = subprocess.Popen(script,
                                        stdout=subprocess.PIPE,
                                        stderr=subprocess.PIPE).communicate()
            logger.debug(
                "Std Err from Release notes generation script {s}: {e}".format(
                    s=script, e=err
                )
            )
            release_notes = out
        except Exception as e:
            logger.error(traceback.format_exc())
            logger.error("Failed to generate release notes: {}".format(e))
            click.get_current_context().exit(-1)
    elif rnfile:
        logger.debug("Reading release notes from file: {}".format(rnfile))

        try:
            release_notes = open(rnfile, "r").read()
        except IOError as e:
            logger.error(traceback.format_exc())
            logger.error("Failed to read release notes file: {}".format(e))
            click.get_current_context().exit(-1)
    elif text:
        logger.debug("Release notes supplied inline: {}".format(text))
        release_notes = text
    else:
        logger.error("One of script, text or file must be specified. "
                     "No release notes generated")
        click.get_current_context().exit(-1)
    click.get_current_context().obj.release_notes = release_notes


@click.command()
def usage():
    commands = ghutils.cli.commands
    for name, command in commands.items():
        print("<br>{}:<br>".format(name))
        help_lines = command.get_help(click.get_current_context()).split('\n')
        for line in help_lines:
            if (not (line.startswith("Usage:") or line.startswith("Options:"))
                    and line):
                print "{}<br>".format(line)


ghutils.cli.add_command(generate_release_notes)
ghutils.cli.add_command(mail)
ghutils.cli.add_command(usage)


if __name__ == "__main__":
    ghutils.cli()
