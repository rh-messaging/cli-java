from __future__ import print_function


import pandas

# ./pict model.pict /o:1

out = open('sender.csv', 'wt')


def add_option(options, option):
    # skip empty value
    if pandas.isnull(option[1]):
        return options
    # stringify
    option = (option[0], str(option[1]))
    # skip non-option keys; the --switch is in the value
    if not option[0].startswith('--'):
        return (option[1],) + options
    # default case
    return option+options


def convert(source_filename, destination_filename):
    with open(destination_filename, 'wt') as destination_file:
        data = pandas.DataFrame.from_csv(source_filename, sep='\t', index_col=None)  # type: pandas.core.frame.DataFrame
        for _, row in data.iterrows():
            args = reduce(add_option, row.iteritems())
            print(args)
            print(" ".join(args), file=destination_file)


if __name__ == '__main__':
    files = ['sender', 'receiver']
    for file in files:
        convert(file + '.out', file + '.csv')
