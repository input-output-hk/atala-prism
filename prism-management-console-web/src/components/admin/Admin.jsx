import React from 'react';
import { Icon } from 'antd';
import PropTypes from 'prop-types';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';

const Admin = ({ populateDemoDataset, isLoading, status }) => (
  <div className="AdminContainer Wrapper">
    <div className="ContentHeader">
      <h1>Environment Admin</h1>
    </div>
    <div className="AdminContent">
      <CustomButton
        buttonProps={{
          className: 'theme-outline',
          onClick: populateDemoDataset,
          disabled: isLoading
        }}
        buttonText="Load demo dataset"
        icon={<Icon type="play-circle" />}
      />
      <div>{status}</div>
    </div>
  </div>
);

Admin.propTypes = {
  populateDemoDataset: PropTypes.func.isRequired,
  isLoading: PropTypes.bool.isRequired,
  status: PropTypes.string.isRequired
};

export default Admin;
