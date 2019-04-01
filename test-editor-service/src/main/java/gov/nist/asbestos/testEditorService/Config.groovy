package gov.nist.asbestos.testEditorService

import groovy.transform.TypeChecked

@TypeChecked
class Config {

    static File cacheDir = new File('/home/bill/asbestos/')

    static File testDefDir = new File(cacheDir, 'testdef')

    static areas = [
            'Test definition directory': testDefDir
    ]

    static init() {
        cacheDir.mkdirs()
        if (!cacheDir.exists())
            throw new Exception("Cache directory ${cacheDir} does not exist and attempt to create it failed")
        if (!cacheDir.isDirectory())
            throw new Exception("Cache directory ${cacheDir} is a simple file instead of a directory")
        if (!cacheDir.canRead())
            throw new Exception("Cache directory ${cacheDir} cannot be read")
        if (!cacheDir.canWrite())
            throw new Exception("Cache directory ${cacheDir} cannot be written")

        areas.each { String title, File dir ->
            dir.mkdirs()
            if (!dir.exists())
                throw new Exception("${title} ${dir} does not exist and attempt to create it failed")
            if (!dir.isDirectory())
                throw new Exception("${title} ${dir} is a simple file instead of a directory")
            if (!dir.canRead())
                throw new Exception("${title} ${dir} cannot be read")
            if (!dir.canWrite())
                throw new Exception("${title} ${dir} cannot be written")
        }

    }
}
