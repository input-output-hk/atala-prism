import React from 'react';
import clsx from 'clsx';
import styles from './HomepageFeatures.module.css';

const FeatureList = [
  {
    title: 'Multiplaftorm',
    Svg: require('../../static/img/kotlin-logo.svg').default,
    description: (
      <>
        Use the same API from your JVM, Android, iOS or JavaScript application. Powered by Kotlin Multiplatform.
      </>
    ),
  },
  {
    title: 'For Effective Governments and Growing Businesses',
    Svg: require('../../static/img/effective-governments.svg').default,
    description: (
      <>
          Implement secure, next-generation services to make your government or business more effective, responsive, and agile.
      </>
    ),
  },
  {
    title: 'Powered by Cardano',
    Svg: require('../../static/img/cardano-logo.svg').default,
    description: (
      <>
          Atala PRISM is built on Cardano, which is based on peer-reviewed academic research and designed for sustainability.
      </>
    ),
  },
];

function Feature({Svg, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} alt={title} />
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
