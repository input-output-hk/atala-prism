import React from "react";
import clsx from 'clsx';
import styles from './index.module.css';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import useThemeContext from '@theme/hooks/useThemeContext';
import HomepageFeatures from "../components/HomepageFeatures";
import BrowserOnly from "@docusaurus/core/lib/client/exports/BrowserOnly";

export default function Home() {
    const SvgLight = require('../../static/img/atala-prism-logo-suite-light.svg').default
    const SvgDark = require('../../static/img/atala-prism-logo-suite-dark.svg').default
    return (
        <Layout
            title={`Atala PRISM SDK`}
            description="Atala PRISM SDK">
            <main>
                <BrowserOnly fallback={<div className={styles.heroLight}/>}>
                    {() => {
                        const {isDarkTheme} = useThemeContext();
                        const heroStyle = isDarkTheme ? styles.heroDark : styles.heroLight;
                        const heroTitleTextStyle = isDarkTheme ? styles.heroTitleTextHtmlDark : styles.heroTitleTextHtmlLight;

                        return (
                            <div className={heroStyle}>
                                <div className={styles.heroInner}>
                                    <h1 className={styles.heroProjectTagline}>
                                        {isDarkTheme ?
                                            <SvgDark alt='PRISM Logo' className={styles.heroLogo}/> :
                                            <SvgLight alt='PRISM Logo' className={styles.heroLogo}/>}
                                        <span className={heroTitleTextStyle}>
                                                Build <b>decentralized</b> identity solutions <b>quickly</b>, regardless of your <b>platform</b>
                                        </span>
                                    </h1>
                                    <div className={styles.indexCtas}>
                                        <Link className="button button--primary" to="/docs">
                                            Get Started
                                        </Link>
                                    </div>
                                </div>
                            </div>
                        )
                    }}
                </BrowserOnly>
                <HomepageFeatures/>
            </main>
        </Layout>
    );
}
