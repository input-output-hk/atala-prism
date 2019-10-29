import React from 'react';
import { withRouter } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

const NotFound = ({ location: { pathname } }) => {
  const { t } = useTranslation();
  const pathnameToShow = pathname.slice(1, pathname.length);

  const error = t('errors.routeDidNotMatch', { pathnameToShow });
  return <div className="App">{error}</div>;
};

NotFound.propTypes = {
  location: PropTypes.shape(PropTypes.Object).isRequired
};

export default withRouter(NotFound);
