import React from 'react';
import { Col, Row } from 'antd';
import flagIcon from '../../../../images/icon-flag.svg';
import avatarId from '../../../../images/avatar-id.png';

import './_style.scss';

const CredentialIDTemplate = () => (
  <div className="CredentialIDTemplate">
    <div className="HeaderTemplate">
      <div className="Headertitle">
        <span>National ID Card</span>
        <p>Republic of Redland</p>
      </div>
      <img className="FlagIcon" src={flagIcon} alt="Flag Icon" />
    </div>
    <div className="ContentTemplate">
      <Row>
        <Col xs={24} lg={6}>
          <img className="AvatarId" src={avatarId} alt="Flag Icon" />
        </Col>
        <Col xs={24} lg={18} className="InfoTemplate">
          <div className="TemplateItem">
            <span>Identity Number</span>
            <p>123 456 789</p>
          </div>
          <div className="TemplateItem">
            <span>Date of Birth</span>
            <p>08/04/1988</p>
          </div>
          <div className="TemplateItem">
            <span>Full Name</span>
            <p>Giorgi Beridze</p>
          </div>
          <div className="TemplateItem">
            <span>Expiration Date</span>
            <p>08/04/2030</p>
          </div>
        </Col>
      </Row>
    </div>
  </div>
);

export default CredentialIDTemplate;
