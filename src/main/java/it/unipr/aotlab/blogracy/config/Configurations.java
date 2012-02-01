/*
 * Copyright (c)  2012 Enrico Franchi, Michele Tomaiuolo and University of Parma.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package it.unipr.aotlab.blogracy.config;

import java.io.File;
import java.net.URL;

/**
 * User: enrico
 * Package: it.unipr.aotlab.blogracy.config
 * Date: 1/24/12
 * Time: 11:37 AM
 */
public class Configurations {

    static public String getTestsStaticFilesRoot() {
        Class<?> myClass = Configurations.class;
        ClassLoader classLoader = myClass.getClassLoader();
        URL url = classLoader.getResource(".");
        return url.getPath();
    }

    public static final String BLOGRACY = "blogracy";
    static String rootDirectoryPath = getTestsStaticFilesRoot();
    static File rootDirectory = new File(rootDirectoryPath);

    static public PathConfig getPathConfig() {
        return new PathConfig() {
            // TODO: this should absolutely come from the outside!

            @Override
            public File getRootDirectory() {
                return rootDirectory;
            }

            @Override
            public String getRootDirectoryPath() {
                return rootDirectory.getAbsolutePath();
            }

            @Override
            public String getStaticFilesDirectoryPath() {
                return getStaticFilesDirectory().getAbsolutePath();
            }

            @Override
            public File getStaticFilesDirectory() {
                return new File(rootDirectory, "static");
            }

            @Override
            public String getTemplatesDirectoryPath() {
                return getTemplatesDirectory().getAbsolutePath();
            }

            @Override
            public File getTemplatesDirectory() {
                return new File(rootDirectory, "templates");
            }


        };
    }
}
