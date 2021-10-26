module.exports = {
    singleQuote: true,
    printWidth: 120,
    tabWidth: 4,
    bracketSpacing: false,
    endOfLine: 'auto',
    overrides: [
        {
            files: '*.java',
            options: {
                tabWidth: 4,
            },
        },
        {
            files: '*.md',
            options: {
                tabWidth: 2,
            },
        },
        {
            files: ['*.yml', '*.yaml'],
            options: {
                tabWidth: 2,
            },
        },
    ],
};
