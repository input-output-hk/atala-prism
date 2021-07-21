import React from 'react';
import Highlight, { defaultProps } from "prism-react-renderer";
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import useThemeContext from '@theme/hooks/useThemeContext';

export default function PrismDependencies() {
    const {siteConfig} = useDocusaurusContext();
    const { isDarkTheme } = useThemeContext();
    const theme = isDarkTheme ? siteConfig.themeConfig.prism.darkTheme : siteConfig.themeConfig.prism.theme;
    const version = siteConfig.customFields.version;
    const code = `// needed for the credential payloads defined in protobuf as well as to interact with our backend services
implementation("io.iohk.atala.prism:protos:${version}")
// needed for cryptography primitives implementation
implementation("io.iohk.atala.prism:crypto:${version}")
// needed to deal with DIDs
implementation("io.iohk.atala.prism:identities:${version}")
// needed to deal with credentials
implementation("io.iohk.atala.prism:credentials:${version}")
// used to avoid some boilerplate
implementation("io.iohk.atala.prism:extras:${version}")

// needed for the credential content, bring the latest version
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
// needed for dealing with dates, bring the latest version
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")`
    return (
        <Highlight Prism={defaultProps.Prism} theme={theme} code={code} language="kotlin">
            {({ className, style, tokens, getLineProps, getTokenProps }) => (
                <pre className={className} style={style}>
                    {tokens.map((line, i) => (
                        <div {...getLineProps({ line, key: i })}>
                            {line.map((token, key) => (
                                <span {...getTokenProps({ token, key })} />
                            ))}
                        </div>
                    ))}
                </pre>
            )}
        </Highlight>
    );
}
