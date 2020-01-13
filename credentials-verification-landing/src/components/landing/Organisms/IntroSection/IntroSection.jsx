import React from 'react';
import { useTranslation } from 'react-i18next';
import IntroItem from '../../Molecules/IntroItem/IntroItem';

import './_style.scss';

const IntroSection = () => {
  const { t } = useTranslation();

  const keys = ['wallet', 'crypto'];

  const separator = <hr className="lineSeparatorSide" />;

  const createIntroItem = (key, pictureNumber) => (
    <IntroItem
      itemIcon={`images/icon-0${pictureNumber}.svg`}
      itemTitle={t(`landing.intro.itemIcon.${key}.title`)}
      itemText={t(`landing.intro.itemIcon.${key}.text`)}
    />
  );

  return (
    <div className="IntroSection">
      <h1>{t('landing.intro.question')}</h1>
      <p>{t('landing.intro.explanation')}</p>
      <div className="IntroItemContainer">
        {keys
          .map((key, index) => createIntroItem(key, index + 2))
          .reduce(
            (acumulator, currentIntroItem) => {
              const withSeparator = acumulator.concat(separator);
              return withSeparator.concat(currentIntroItem);
            },
            [createIntroItem('credentials', 1)]
          )}
      </div>
    </div>
  );
};

export default IntroSection;
