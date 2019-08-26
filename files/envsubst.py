import argparse
import os

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Use envsubst to substitute content')
    parser.add_argument('input', metavar='i', type=str, help='inputfile')
    parser.add_argument("-o", action='store',
                        type=argparse.FileType('w'), dest='output',
                        help="redirect substituted content to file (default: same as input)")
    parser.add_argument('-env', action='append',
                        help='Only replace this variable (can be used multiple times)',
                        default=[])

    args = parser.parse_args()
    if not args.output:
        args.output = args.input

    envlist = list(map(lambda x: f'${x}', args.env))
    env = ",".join(envlist)
    if env:
        env = f'\'{env}\''

    os.system(f'envsubst {env}< {args.input} > {args.output}')
