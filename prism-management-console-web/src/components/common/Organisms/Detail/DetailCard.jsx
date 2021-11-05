import React from 'react';
import PropTypes from 'prop-types';
import { Card } from 'antd';

const DetailCard = ({ title, badge, info, children }) => (
  <div className="CredentialStatusCard">
    <Card title={title} extra={badge}>
      {info}
      {children}
    </Card>
  </div>
);

DetailCard.defaultProps = {
  title: undefined,
  badge: undefined,
  info: null,
  children: null
};

DetailCard.propTypes = {
  title: PropTypes.string,
  badge: PropTypes.node,
  info: PropTypes.node,
  children: PropTypes.node
};

export default DetailCard;
