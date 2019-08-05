# onCreate #

Performs actions when a file is created in a watched directory tree.

Usage: `OnCreate [dirName]`

If `dirName` is not specified then the current directory is watched.

## Configuration file ##

Occurrences of `$f` in `commandTokens` will be replaced by the fully qualified name of the newly created file.
The following action map will cause newly created files with type `.m4a` to be passed to a program called `m4aToWav`, and 
newly created files with type `.mov` to be passed to a program called `movToMono`.

````
{
    "actionMap": [
        {
            "filetype": "m4a",
            "commandTokens": ["m4aToWav", "$f"]
        },
        {
            "filetype": "mov",
            "commandTokens": ["movToMono", "$f"]
        }
    ]
}
````
