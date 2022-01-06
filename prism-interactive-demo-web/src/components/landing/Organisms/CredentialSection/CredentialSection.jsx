import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';

import './_style.scss';

export const items = [
  { title: 'landing.credential.titlecomponent1', description: 'landing.credential.component1' },
  { title: 'landing.credential.titlecomponent3', description: 'landing.credential.component3' },
  { title: 'landing.credential.titlecomponent2', description: 'landing.credential.component2' },
  { title: 'landing.credential.titlecomponent4', description: 'landing.credential.component4' },
  {
    title: 'landing.credential.titlecomponent5',
    description: 'landing.credential.component5',
    extra: 'landing.credential.title7'
  },
  {
    title: 'landing.credential.titlecomponent6',
    description: 'landing.credential.component6',
    extra: 'landing.credential.title7'
  }
];

const CredentialSection = () => {
  const { t } = useTranslation();

  return (
    <div className="ComponentsPanel">
      <div className="CredentialSection">
        <div className="TextContainer">
          <h1>{t('landing.credential.title')}</h1>
          <div className="ComponentsContainer">
            {items.map(({ title, description, extra }) => (
              <div className="ComponentItem" key={title}>
                <h3>{t(title)}</h3>
                {extra && <p className="ComingSoon">{t(extra)}</p>}
                <p>{t(description)}</p>
              </div>
            ))}
          </div>
        </div>
        <div className="ImageContainer">
          <img
            src="/images/illustration-components.svg"
            alt={t('landing.credential.credentialAlt')}
          />
        </div>
      </div>
      <div className="triangle-down" />
    </div>
  );
};

export default CredentialSection;
