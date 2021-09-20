import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import IntroItem from '../../Molecules/IntroItem/IntroItem';

import './_style.scss';
import ItemCollapse from '../../Molecules/ItemCollapse/ItemCollapse';

const IntroSection = () => {
  const { t } = useTranslation();

  const keys = ['wallet', 'crypto'];

  const separator = key => <hr key={key} className="lineSeparatorSide" />;

  const createIntroItem = (key, pictureNumber) => (
    <div className="ItemVision">
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
          .map((key, index) => createIntroItem(key, index + 2))
          .reduce(
            (acumulator, currentIntroItem) => {
              const withSeparator = acumulator.concat(separator(acumulator.length));
              return withSeparator.concat(currentIntroItem);
            },
            [createIntroItem('credentials', 1)]
          )}
      </div>
    </div>
  );
};

export default IntroSection;
