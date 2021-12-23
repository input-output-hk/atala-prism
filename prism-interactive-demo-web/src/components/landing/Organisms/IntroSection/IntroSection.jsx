import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import IntroItem from '../../Molecules/IntroItem/IntroItem';
import ItemCollapse from '../../Molecules/ItemCollapse/ItemCollapse';

import './_style.scss';

const IntroSection = () => {
  const { t } = useTranslation();

  const keys = ['wallet', 'crypto'];

  const separator = key => <hr key={key} className="lineSeparatorSide" />;

  const createIntroItem = key => (
    <div className="ItemVision" key={key}>
      <IntroItem
        key={key}
        itemIcon={t(`landing.intro.itemIcon.${key}.image`)}
        itemTitle={t(`landing.intro.itemIcon.${key}.title`)}
        itemText={t(`landing.intro.itemIcon.${key}.text`)}
      />
      <ItemCollapse name={key} />
    </div>
  );

  return (
    <div className="IntroSection">
      <h1>{t('landing.intro.question')}</h1>
      <div className="IntroItemContainer">
        {keys
          .map(key => createIntroItem(key))
          .reduce(
            (acumulator, currentIntroItem) => {
              const withSeparator = acumulator.concat(separator(acumulator.length));
              return withSeparator.concat(currentIntroItem);
            },
            [createIntroItem('credentials')]
          )}
      </div>
    </div>
  );
};

export default IntroSection;
