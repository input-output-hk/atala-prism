import React from 'react';
import { Card } from 'antd';

const DetailCard = ({ title, badge, info, children }) => (
  <div className="CredentialStatusCard">
    <Card title={title} extra={badge}>
      {info}
      {children}
    </Card>
  </div>
);
export default DetailCard;
