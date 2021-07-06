import React from 'react';
import { Divider } from 'antd';
import PropTypes from 'prop-types';
import imgCard from '../../../images/contactIcon.svg';
import BulletItems from '../Molecules/DashboardCardsItems/BulletItems';
import './_style.scss';
import { useTranslationWithPrefix } from '../../../hooks/useTranslationWithPrefix';

const DashboardCard = ({ loading, data }) => {
  const tp = useTranslationWithPrefix('dashboard.analytics');

  const contactsPendingConnection = data.numberOfContacts - data.numberOfContactsConnected;

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
        value={contactsPendingConnection}
        loading={loading}
      />
      <BulletItems
        bulletClass="greenBullet"
        title={tp('connected')}
        value={data.numberOfContactsConnected}
        loading={loading}
      />
      <BulletItems
        bulletClass="violetBullet"
        title={tp('total')}
        value={data.numberOfContacts}
        loading={loading}
      />
    </div>
  );
};

DashboardCard.defaultProps = {
  loading: false,
  data: {
    numberOfContactsPendingConnection: 0,
    numberOfContactsConnected: 0,
    numberOfContacts: 0
  }
};

DashboardCard.propTypes = {
  loading: PropTypes.bool,
  data: PropTypes.shape({
    numberOfContactsPendingConnection: PropTypes.number,
    numberOfContactsConnected: PropTypes.number,
    numberOfContacts: PropTypes.number
  })
};

export default DashboardCard;
