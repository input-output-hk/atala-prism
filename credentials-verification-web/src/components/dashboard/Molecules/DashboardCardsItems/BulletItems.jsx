import React from 'react';
import { useTranslation } from 'react-i18next';
import './_style.scss';

const BulletItems = ({ bulletClass, title, value }) => (
  <div className="BulletItemsContainer">
    <div className="textTitleContainer">
      <div className={`bullet ${bulletClass}`} />
      <div className="titleContainer">
        <span>{title}</span>
      </div>
    </div>
    <div className="valueContainer">
      <h1>{value}</h1>
    </div>
  </div>
);

export default BulletItems;
