import React, { useState } from 'react';
import _ from 'lodash';
import { Drawer, message, Tabs, Button } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import { drawerWidth } from '../../../../helpers/constants';
import img from '../../../../images/verified.svg';
import cardanoLogo from '../../../../images/cardanoLogo.svg';
import arrow from '../../../../images/right.svg';
import hashed from '../../../../images/hashed.svg';
import hashedFile from '../../../../images/hashedFile.svg';
import CardDetail from './DetailCard';
import DataDetail from './DataDetail';
import { sanitizeView } from '../../../../helpers/credentialView';
import CredentialRawDetail from './CredentialRawDetail/CredentialRawDetail';

const { TabPane } = Tabs;

const CARDANO_MAINNET_LEDGER = 5;

const isMainnet = ledger => ledger === CARDANO_MAINNET_LEDGER;

const getCardanoExplorerUrl = (txId, ledger) => {
  const baseUrl = isMainnet(ledger)
    ? 'https://explorer.cardano.org/en/transaction?id='
    : 'https://explorer.cardano-testnet.iohkdev.io/en/transaction?id=';
  return baseUrl.concat(txId);
};

const CredentialSummaryDetail = ({ drawerInfo, credentialData }) => {
  const { t } = useTranslation();
  const [rawVisible, setRawVisible] = useState(false);

  const tabs = {
    summary: {
      key: 1,
      title: t('credentials.drawer.summary.title')
    },
    details: {
      key: 2,
      title: t('credentials.drawer.details.title')
    }
  };

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
          className: 'theme-link',
          onClick: () => setRawVisible(true)
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

  const renderHtmlCredential = ({ html }) => {
    const unescapedHtml = _.unescape(html);
    const cleanHtml = sanitizeView(unescapedHtml);
    /* eslint-disable-next-line react/no-danger */
    return <div dangerouslySetInnerHTML={{ __html: cleanHtml }} />;
  };

  const handleVerify = () => {
    message.warn(t('credentials.messages.notImplementedYet'));
  };

  const { transactionid, ledger } = credentialData?.issuanceproof || {};

  return (
    <Drawer
      className="credentialDetailDrawer"
      placement="right"
      width={drawerWidth}
      destroyOnClose
      {...drawerInfo}
    >
      <Tabs defaultActiveKey={tabs.details.key} centered>
        <TabPane tab={tabs.summary.title} key={tabs.summary.key}>
          <div className="credentialContainer">{renderHtmlCredential({ ...credentialData })}</div>
          {/* Left commented code for when it's possible verify credential */}
          {/* <div className="actionsContainer">
            <CustomButton
              buttonText={t('credentials.detail.verify')}
              buttonProps={{
                className: 'theme-primary verifyButton',
                onClick: handleVerify
              }}
            />
          </div> */}
        </TabPane>
        <TabPane tab={tabs.details.title} key={tabs.details.key}>
          <CardDetail
            title={t('credentials.detail.statusTitle')}
            info={infoFirstCard}
            badge={extraJsx}
          >
            <DataDetail
              img={hashedFile}
              title={t('credentials.detail.hashedFile')}
              data={t('credentials.detail.rawCredential')}
              contentPopOver={content}
            />
          </CardDetail>
          <CardDetail title={t('credentials.detail.notarizationTitle')} info={infoSecondCard}>
            <DataDetail
              img={hashed}
              title={t('credentials.detail.fileHash')}
              data="#a4b412fdf47dfd457djhgf3..."
              contentPopOver={contentSecondCard}
            />
            <DataDetail
              img={hashed}
              title={t('credentials.detail.hashTitle')}
              data={transactionid ? `#${transactionid}` : '-'}
              contentPopOver={contentSecondCard}
            />

            <Button
              className="cardanoContainer"
              href={getCardanoExplorerUrl(transactionid, ledger)}
              target="_blank"
              disabled={!transactionid}
            >
              <div>
                <img src={cardanoLogo} alt="CardanoLogo" />
                <span>{t('credentials.detail.viewCardanoExplorer')}</span>
              </div>
              <div>
                <img src={arrow} alt="arrow" />
              </div>
            </Button>
          </CardDetail>
        </TabPane>
      </Tabs>

      <CredentialRawDetail
        visible={rawVisible}
        credentialString={credentialData.credentialdata}
        onClose={() => setRawVisible(false)}
      />
    </Drawer>
  );
};

CredentialSummaryDetail.propTypes = {
  drawerInfo: PropTypes.shape().isRequired,
  credentialData: PropTypes.shape({
    html: PropTypes.string,
    credentialdata: PropTypes.string,
    issuanceproof: PropTypes.shape({ transactionid: PropTypes.string })
  }).isRequired
};

export default CredentialSummaryDetail;
