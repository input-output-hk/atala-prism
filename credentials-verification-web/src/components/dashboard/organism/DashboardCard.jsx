import React from 'react';
import { useTranslation } from 'react-i18next';
import './_style.scss';
import { Divider } from 'antd';
import imgCard from '../../../images/contactIcon.svg';
import BulletItems from '../Molecules/DashboardCardsItems/BulletItems';

const DashboardCard = () => (
  <div className="DashboardContactCardContainer">
    <div className="HeaderCardContainer">
      <div>
        <img src={imgCard} alt="img" srcSet="" />
      </div>
      <span className="titleCardContainer">Contacts</span>
    </div>
    <div className="divider">
      <Divider />
    </div>
    <BulletItems bulletClass="beigeBullet" title="Pending Connection" value="27" />
    <BulletItems bulletClass="greenBullet" title="Connected" value="3" />
    <BulletItems bulletClass="violetBullet" title="Total" value="30" />
  </div>
);

export default DashboardCard;
