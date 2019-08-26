import argparse
import os

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Use envsubst to substitute content')
    parser.add_argument('input', metavar='i', type=str, help='inputfile')
    parser.add_argument("-o", action='store',
                        type=argparse.FileType('w'), dest='output',
                        help="redirect substituted content to file (default: same as input)")
    args = parser.parse_args()
    if not args.output:
        args.output = args.input

    os.system(f'envsubst < {args.input} > {args.output}')
