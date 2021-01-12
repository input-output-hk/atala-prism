import React from 'react';
import PropTypes from 'prop-types';
import './_style.scss';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';

const BulletItems = ({ bulletClass, title, value, loading }) => (
  <div className="BulletItemsContainer">
    <div className="textTitleContainer">
      <div className={`bullet ${bulletClass}`} />
      <div className="titleContainer">
        <span>{title}</span>
      </div>
    </div>
    <div className="valueContainer">{loading ? <SimpleLoading size="xs" /> : <h1>{value}</h1>}</div>
  </div>
);

BulletItems.defaultProps = {
  loading: false,
  bulletClass: '',
  value: 0
};

BulletItems.propTypes = {
  loading: PropTypes.bool,
  bulletClass: PropTypes.string,
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
};

export default BulletItems;
