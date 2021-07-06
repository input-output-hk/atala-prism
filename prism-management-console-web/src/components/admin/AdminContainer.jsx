import React, { useState } from 'react';
import { Redirect } from 'react-router-dom';
import PropTypes from 'prop-types';
import { withApi } from '../providers/withApi';
import Admin from './Admin';

const AdminContainer = ({ api }) => {
  const [isLoading, setIsLoading] = useState(false);
  const [status, setStatus] = useState('');

  const populateDemoDataset = () => {
    setIsLoading(true);
    return api.admin.populateDemoDataset().then(
      result => {
        setIsLoading(false);
        setStatus(result.getMessage());
      },
      err => {
        setIsLoading(false);
        setStatus('There was an error: ' + err.message);
      }
    );
  };

  if (api.admin.isAdminSupported()) {
    return (
      <Admin populateDemoDataset={populateDemoDataset} isLoading={isLoading} status={status} />
    );
  }
  return <Redirect to={{ pathname: '/' }} />;
};

AdminContainer.propTypes = {
  api: PropTypes.shape({
    admin: PropTypes.shape({
      isAdminSupported: PropTypes.func,
      populateDemoDataset: PropTypes.func
    }).isRequired
  }).isRequired
};

export default withApi(AdminContainer);
