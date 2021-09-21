import React from 'react';
import { InfoCircleOutlined } from '@ant-design/icons';
import { Drawer, Card, Row, Col, Divider } from 'antd';
import PropTypes from 'prop-types';
import { useTranslationWithPrefix } from '../../../../../hooks/useTranslationWithPrefix';
import JsonView from '../../../Atoms/JsonView/JsonView';
import './_style.scss';
import DownloadRawButton from '../../../Atoms/DownloadRawButton/DownloadRawButton';

const CredentialRawDetail = ({ credentialString, visible, onClose, downloadProps }) => {
  const tp = useTranslationWithPrefix('credentials.drawer.raw');

  const credentialJson = JSON.parse(credentialString);

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
              <InfoCircleOutlined className="icon" style={{ fontSize: 16 }} />
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
        <DownloadRawButton overrideClassName="theme-primary full-width" {...downloadProps} />
      </div>
    </Drawer>
  );
};

CredentialRawDetail.propTypes = {
  credentialString: PropTypes.string.isRequired,
  visible: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  downloadProps: PropTypes.shape({
    downloadHref: PropTypes.string,
    downloadName: PropTypes.string,
    disabled: PropTypes.bool
  }).isRequired
};

export default CredentialRawDetail;
