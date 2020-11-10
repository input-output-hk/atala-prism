import React, { Fragment } from 'react';
import { Drawer, Icon, Tabs, Card, Popover, Button } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialSummaryData from '../../Atoms/CredentialData/CredentialSummaryData';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import { drawerWidth, VERIFIER } from '../../../../helpers/constants';
import { useSession } from '../../../providers/SessionContext';

import img from '../../../../images/verified.svg';
import cardanoLogo from '../../../../images/cardanoLogo.svg';
import arrow from '../../../../images/right.svg';
import hashed from '../../../../images/hashed.svg';
import hashedFile from '../../../../images/hashedFile.svg';
import CardDetail from './DetailCard';
import DataDetail from './DataDetail';

const { TabPane } = Tabs;

const CredentialSummaryDetail = ({ drawerInfo, credentialData }) => {
  const { t } = useTranslation();
  const { session } = useSession();

  const role = session.userRole;
  const university = session.organizationName;

  const content = (
    <div>
      <CustomButton
        buttonProps={{
          className: 'theme-link'
        }}
        buttonText="Download"
      />
      <CustomButton
        buttonProps={{
          className: 'theme-link'
        }}
        buttonText="View"
      />
    </div>
  );

  const contentSecondCard = (
    <CustomButton
      buttonProps={{
        className: 'theme-link'
      }}
      buttonText="Copy"
    />
  );

  const extraJsx = (
    <div className="ValidityButton">
      <img src={img} alt="verifiedIcon" />
      <span>{t('credentials.detail.validity')}</span>
      <p>{t('credentials.detail.valid')}</p>
    </div>
  );

  const infoFirstCard = (
    <>
      <div className="credentialStatusContent">
        <span>{t('credentials.detail.source')}</span>
        <p>University of Innovation and Technology</p>
      </div>
      <div className="credentialStatusContent">
        <span>{t('credentials.detail.integrity')}</span>
        <p> No changes </p>
      </div>
    </>
  );

  const infoSecondCard = (
    <div className="credentialStatusContent">
      <h5 className="">{t('credentials.detail.transactionTimestamp')} 23/08/2019 | 15:00 hs </h5>
    </div>
  );

  return (
    <Drawer
      className="credentialDetailDrawer"
      placement="right"
      width={drawerWidth}
      destroyOnClose
      {...drawerInfo}
    >
      <Fragment>
        <Tabs defaultActiveKey="1">
          <TabPane tab="View" key="1">
            {credentialData && (
              <CredentialSummaryData {...credentialData} university={university} />
            )}
            {role === VERIFIER && (
              <CustomButton
                buttonProps={{ className: 'theme-outline', onClick: () => {} }}
                icon={<Icon type="download" />}
              />
            )}
            {role === VERIFIER && (
              <CustomButton
                buttonProps={{ className: 'theme-secondary', onClick: () => {} }}
                buttonText={t('credentials.detail.verify')}
              />
            )}
          </TabPane>
          <TabPane tab="Verification Details" key="2">
            <CardDetail title="Credential Status" info={infoFirstCard} badge={extraJsx}>
              {
                <DataDetail
                  img={hashedFile}
                  title={t('credentials.detail.hashedFile')}
                  data="Raw Credential"
                  contentPopOver={content}
                />
              }
            </CardDetail>
            <CardDetail title="Blockchain Notarization" info={infoSecondCard}>
              <DataDetail
                img={hashed}
                title={t('credentials.detail.fileHash')}
                data="#a4b412fdf47dfd457djhgf3..."
                contentPopOver={contentSecondCard}
              />
              <DataDetail
                img={hashed}
                title={t('credentials.detail.hashTitle')}
                data="#a4b412fdf47dfd457djhgf3..."
                contentPopOver={contentSecondCard}
              />

              <div className="cardanoContainer">
                <div>
                  <img src={cardanoLogo} alt="CardanoLogo" />
                  <span>{t('credentials.detail.viewCardanoExplorer')}</span>
                </div>
                <div>
                  <img src={arrow} alt="arrow" />
                </div>
              </div>
            </CardDetail>
          </TabPane>
        </Tabs>
      </Fragment>
    </Drawer>
  );
};

CredentialSummaryDetail.propTypes = {
  drawerInfo: PropTypes.shape().isRequired,
  credentialData: PropTypes.shape().isRequired
};

export default CredentialSummaryDetail;
