import React from 'react';
import { Drawer, Card, Row, Col, Icon, Divider } from 'antd';
import PropTypes from 'prop-types';
import { useTranslationWithPrefix } from '../../../../../hooks/useTranslationWithPrefix';
import CustomButton from '../../../Atoms/CustomButton/CustomButton';
import JsonView from '../../../Atoms/JsonView/JsonView';
import './_style.scss';

const CredentialRawDetail = ({ credentialString, visible, onClose }) => {
  const tp = useTranslationWithPrefix('credentials.drawer.raw');

  const credentialJson = JSON.parse(credentialString);

  const { contactName, credentialType, cardNumber } = credentialJson;
  const downloadHref = `data:text/plain;charset=utf-8,${encodeURIComponent(credentialString)}`;
  const downloadName = `${contactName}-${credentialType}-${cardNumber}.txt`.replace(' ', '_');

  return (
    <Drawer
      title={tp('title')}
      width={460}
      onClose={onClose}
      visible={visible}
      className="CredentialRawDetail"
    >
      <div className="px-2">
        <div className="mb-3">
          <JsonView src={credentialJson} />
        </div>

        <Card className="Card">
          <Row>
            <Col span={2}>
              <Icon type="info-circle" className="icon" style={{ fontSize: 16 }} />
            </Col>
            <Col span={22}>
              <Divider orientation="left" className="title">
                {tp('important')}
              </Divider>
              <div className="description">
                <p>{tp('descriptionFirst')}</p>
                <p>{tp('descriptionSecond')}</p>
              </div>
            </Col>
          </Row>
        </Card>

        <a href={downloadHref} download={downloadName}>
          <CustomButton
            buttonText={tp('downloadFile')}
            buttonProps={{ className: 'theme-primary w-100' }}
          />
        </a>
      </div>
    </Drawer>
  );
};

CredentialRawDetail.propTypes = {
  credentialString: PropTypes.string.isRequired,
  visible: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired
};

export default CredentialRawDetail;
