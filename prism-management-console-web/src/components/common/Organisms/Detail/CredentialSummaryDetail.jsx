import React, { useState } from 'react';
import _ from 'lodash';
import { Drawer, Tabs, Alert, Divider } from 'antd';
import { Trans, useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import { CREDENTIAL_STATUSES, drawerWidth } from '../../../../helpers/constants';
import verifiedIcon from '../../../../images/verified.svg';
import invalidIcon from '../../../../images/redCross.svg';
import cardanoLogo from '../../../../images/cardanoLogo.svg';
import hashedFile from '../../../../images/hashedFile.svg';
import CardDetail from './DetailCard';
import DataDetail from './DataDetail';
import { sanitizeView } from '../../../../helpers/credentialView';
import CredentialRawDetail from './CredentialRawDetail/CredentialRawDetail';
import revokedIconSrc from '../../../../images/revokeIcon.svg';

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

  const {
    verificationResult: {
      credentialRevoked,
      batchRevoked,
      invalidMerkleProof,
      invalidKey,
      keyRevoked,
      invalidSignature
    } = {}
  } = credentialData;

  const credentialIsValid = !(
    credentialRevoked ||
    batchRevoked ||
    invalidMerkleProof ||
    invalidKey ||
    keyRevoked ||
    invalidSignature
  );

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

  const extraJsx = (
    <div className={credentialIsValid ? 'ValidButton' : 'InvalidButton'}>
      <img src={credentialIsValid ? verifiedIcon : invalidIcon} alt="verifiedIcon" />
      <span>{t('credentials.detail.validity')}</span>
      <p>{t(`credentials.detail.${credentialIsValid ? 'valid' : 'invalid'}`)}</p>
    </div>
  );

  const infoFirstCard = (
    <>
      <div className="credentialStatusContent">
        <span>{t('credentials.detail.source')}</span>
        <p>{credentialData?.issuer}</p>
      </div>
      <div className="credentialStatusContent">
        <span>{t('credentials.detail.integrity.title')}</span>
        <p>{t(`credentials.detail.integrity.${invalidSignature ? 'invalid' : 'valid'}`)}</p>
      </div>
    </>
  );

  const renderHtmlCredential = ({ html }) => {
    const unescapedHtml = _.unescape(html);
    const cleanHtml = sanitizeView(unescapedHtml);
    /* eslint-disable-next-line react/no-danger */
    return <div dangerouslySetInnerHTML={{ __html: cleanHtml }} />;
  };

  const cardanoExplorerHeader = (
    <div className="CardanoExplorerHeader">
      <img className="CardanoLogo" src={cardanoLogo} alt="CardanoLogo" />
      <span>{t('credentials.detail.viewCardanoExplorer')}</span>
    </div>
  );

  const { transactionid, ledger } = credentialData?.issuanceproof || {};

  const revokedIcon = <img className="revokedIcon" src={revokedIconSrc} alt="revoked icon" />;

  const revokedMessage = (
    <Trans i18nKey="credentials.detail.revokedAlert">
      This Credential <strong>has been Revoked</strong> by the Issuing Authority
    </Trans>
  );

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
          {credentialData.status === CREDENTIAL_STATUSES.credentialRevoked && (
            <div className="revokedAlertContainer">
              <Divider />
              <Alert message={revokedMessage} type="error" showIcon icon={revokedIcon} />
            </div>
          )}
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
          <CardDetail title={cardanoExplorerHeader}>
            <CustomButton
              buttonProps={{
                className: 'GoToCardanoBtn theme-primary',
                target: '_blank',
                href: getCardanoExplorerUrl(transactionid, ledger),
                disabled: !transactionid
              }}
              buttonText={t('credentials.detail.goToCardano')}
            />
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
    issuanceproof: PropTypes.shape({ transactionid: PropTypes.string }),
    status: PropTypes.number.isRequired,
    verificationResult: PropTypes.shape({
      credentialRevoked: PropTypes.bool,
      batchRevoked: PropTypes.bool,
      invalidMerkleProof: PropTypes.bool,
      invalidKey: PropTypes.bool,
      keyRevoked: PropTypes.bool,
      invalidSignature: PropTypes.bool
    })
  }).isRequired
};

export default CredentialSummaryDetail;
