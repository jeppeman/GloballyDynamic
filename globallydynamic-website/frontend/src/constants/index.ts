const pageNames: { [key: string]: string[] } = {
    '/docs/user': ['Docs', 'Android'],
    '/docs/user/android': ['Documentation', 'Android Lib'],
    '/docs/user/server': ['Documentation', 'Server Lib'],
    '/docs/user/gradle': ['Documentation', 'Gradle Plugin'],
    '/docs/user/studio': ['Documentation', 'Studio Plugin'],
    '/docs/javadoc/android': ['Javadoc', 'Android Lib'],
    '/docs/javadoc/server': ['Javadoc', 'Server Lib'],
    '/user-guide/getting-started/development': ['User Guide', 'Getting Started', 'Development Setup'],
    '/user-guide/getting-started/complete': ['User Guide', 'Getting Started', 'Complete Setup'],
    '/user-guide/server': ['User Guide', 'Dedicated Server'],
    '/user-guide/amazon-app-store': ['User Guide', 'Amazon App Store'],
    '/user-guide/samsung-galaxy-store': ['User Guide', 'Samsung Galaxy Store'],
    '/user-guide/huawei-app-gallery': ['User Guide', 'Huawei App Gallery'],
    '/user-guide/firebase-app-distribution': ['User Guide', 'Firebase App Distribution'],
    '/user-guide/troubleshooting': ['User Guide', 'Troubleshooting']
}

export type Versions = { AGP: string, GRADLE: string, ANDROID: string, SERVER: string, STUDIO: string }

const versions: Versions = {
    'GRADLE': '1.8.0',
    'ANDROID': '1.3.0',
    'SERVER': '1.5.0',
    'STUDIO': '1.11.0',
    'AGP': '8.1.1'
}

export {
    pageNames,
    versions
}
