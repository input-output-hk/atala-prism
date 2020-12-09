import React from 'react';
import './_style.scss';
import { Divider } from 'antd';
import PropTypes from 'prop-types';
import imgCard from '../../../images/groupIcon.svg';
import BulletItems from '../Molecules/DashboardCardsItems/BulletItems';
import { useTranslationWithPrefix } from '../../../hooks/useTranslationWithPrefix';

const DashboardCardGroup = ({ loading, data }) => {
  const tp = useTranslationWithPrefix('dashboard.analytics');
  return (
    <div className="DashboardContactCardContainer">
      <div className="HeaderCardContainer">
        <div>
          <img src={imgCard} alt="img" srcSet="" />
        </div>
        <span className="titleCardContainer">{tp('groups')}</span>
      </div>
      <div className="divider">
        <Divider />
      </div>
      <BulletItems
        bulletClass="violetBullet"
        title={tp('total')}
        value={data.numberofgroups}
        loading={loading}
      />
    </div>
  );
};

DashboardCardGroup.defaultProps = {
  loading: false,
  data: {
    numberofgroups: 0
  }
};

DashboardCardGroup.propTypes = {
  loading: PropTypes.bool,
  data: PropTypes.shape({
    numberofgroups: PropTypes.number
  })
};

export default DashboardCardGroup;
