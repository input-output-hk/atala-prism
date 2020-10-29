import React from 'react';
import { useTranslation } from 'react-i18next';
import './_style.scss';
import { Divider } from 'antd';
import imgCard from '../../../images/groupIcon.svg';
import BulletItems from '../Molecules/DashboardCardsItems/BulletItems';

const DashboardCardGroup = () => (
  <div className="DashboardContactCardContainer">
    <div className="HeaderCardContainer">
      <div>
        <img src={imgCard} alt="img" srcSet="" />
      </div>
      <span className="titleCardContainer">Groups</span>
    </div>
    <div className="divider">
      <Divider />
    </div>
    <BulletItems bulletClass="violetBullet" title="Total" value="10" />
  </div>
);

export default DashboardCardGroup;
