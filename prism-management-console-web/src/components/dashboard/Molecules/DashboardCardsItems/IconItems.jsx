import React from 'react';
import PropTypes from 'prop-types';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import './_style.scss';

const IconItems = ({ icon, title, value, loading }) => (
  <div className="BulletItemsContainer">
    <div className="textTitleContainer">
      <div className="icon">
        <img src={icon} alt="icon" />
      </div>
      <div className="titleContainer">
        <span>{title}</span>
      </div>
    </div>
    <div className="valueContainer">{loading ? <SimpleLoading size="xs" /> : <h1>{value}</h1>}</div>
  </div>
);

IconItems.defaultProps = {
  loading: false,
  value: 0
};

IconItems.propTypes = {
  loading: PropTypes.bool,
  icon: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
};

export default IconItems;
