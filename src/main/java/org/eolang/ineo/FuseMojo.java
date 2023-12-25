/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2023 Objectionary.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.eolang.ineo;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XSL;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.cactoos.io.ResourceOf;
import org.cactoos.text.TextOf;
import org.xembly.Directives;
import org.xembly.ImpossibleModificationException;
import org.xembly.Xembler;

/**
 * Fuse {@code new B(new A(42).bar()} java code into {@code new BA(42).bar()}.
 * @since 0.1
 */
@Mojo(
    name = "fuse",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true
)
public final class FuseMojo extends AbstractMojo {
    /**
     * Transformation.
     */
    private static final XSL TRANSFORMATION = new XSLDocumentOf(
        new TextOf(
            new ResourceOf("org/eolang/ineo/fuse/fuse.xsl")
        )
    );

    /**
     * Fused XMIR.
     */
    private static final XML FUSED = new XMLDocumentOf(
        new TextOf(
            new ResourceOf("org/eolang/ineo/fuse/BA.xmir")
        )
    );

    /**
     * Sources directory.
     * @checkstyle MemberNameCheck (10 lines)
     */
    @Parameter(
        property = "sourcesDir",
        required = true,
        defaultValue = "${basedir}/target/generated-sources/xmir"
    )
    private File sourcesDir;

    @Override
    public void execute() {
        for (final Path file : new FilesOf(this.sourcesDir)) {
            final XML before = new XMLDocumentOf(file);
            final XML after = FuseMojo.TRANSFORMATION.transform(before);
            if (!before.equals(after)) {
                try {
                    final String pckg = this.sourcesDir.toPath().relativize(file).toString()
                        .replace(String.format("%s%s", File.separator, file.getFileName()), "");
                    new Home(
                        new Saved(after, file),
                        new Saved(
                            new XMLDocument(
                                new Xembler(
                                    new Directives()
                                        .append(FuseMojo.FUSED.node())
                                        .xpath("//metas/meta[head/text()='package']/tail")
                                        .set(pckg)
                                        .xpath("//metas/meta[head/text()='package']/part")
                                        .set(pckg)
                                ).xml()
                            ),
                            this.sourcesDir.toPath().resolve(
                                String.join(File.separator, pckg, "BA.xmir")
                            )
                        )
                    ).save();
                } catch (final IOException ex) {
                    throw new IllegalStateException("Couldn't save file after transformation", ex);
                } catch (final ImpossibleModificationException ex) {
                    throw new IllegalStateException("Couldn't transform fused XMIR", ex);
                }
            }
        }
    }
}