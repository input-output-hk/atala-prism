import React from 'react';
import { Divider } from 'antd';
import PropTypes from 'prop-types';
import imgCard from '../../../images/contactIcon.svg';
import BulletItems from '../Molecules/DashboardCardsItems/BulletItems';
import './_style.scss';
import { useTranslationWithPrefix } from '../../../hooks/useTranslationWithPrefix';

const DashboardCard = ({ loading, data }) => {
  const tp = useTranslationWithPrefix('dashboard.analytics');

  return (
    <div className="DashboardContactCardContainer">
      <div className="HeaderCardContainer">
        <div>
          <img src={imgCard} alt="img" srcSet="" />
        </div>
        <span className="titleCardContainer">{tp('contacts')}</span>
      </div>
      <div className="divider">
        <Divider />
      </div>
      <BulletItems
        bulletClass="beigeBullet"
        title={tp('pendingConnection')}
        value={data.numberofcontactspendingconnection}
        loading={loading}
      />
      <BulletItems
        bulletClass="greenBullet"
        title={tp('connected')}
        value={data.numberofcontactsconnected}
        loading={loading}
      />
      <BulletItems
        bulletClass="violetBullet"
        title={tp('total')}
        value={data.numberofcontacts}
        loading={loading}
      />
    </div>
  );
};

DashboardCard.defaultProps = {
  loading: false
};

DashboardCard.propTypes = {
  loading: PropTypes.bool,
  data: PropTypes.shape({
    numberofcontactspendingconnection: PropTypes.number,
    numberofcontactsconnected: PropTypes.number,
    numberofcontacts: PropTypes.number
  }).isRequired
};

export default DashboardCard;
