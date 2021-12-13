import React, { useState } from 'react';
import { Drawer, Tabs, Alert, Divider } from 'antd';
import { Trans, useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import { CREDENTIAL_STATUSES, drawerWidth } from '../../../../helpers/constants';
import verifiedIcon from '../../../../images/verified.svg';
import invalidIcon from '../../../../images/redCross.svg';
import pendingIcon from '../../../../images/pendingIcon.svg';
import cardanoLogo from '../../../../images/cardanoLogo.svg';
import hashedFile from '../../../../images/hashedFile.svg';
import DetailCard from './DetailCard';
import DataDetail from './DataDetail';
import { sanitizeView } from '../../../../helpers/credentialView';
import CredentialRawDetail from './CredentialRawDetail/CredentialRawDetail';
import revokedIconSrc from '../../../../images/revokeIcon.svg';
import { credentialReceivedShape, credentialShape } from '../../../../helpers/propShapes';
import DownloadRawButton from '../../Atoms/DownloadRawButton/DownloadRawButton';

const { TabPane } = Tabs;

const CARDANO_MAINNET_LEDGER = 5;

const isMainnet = ledger => ledger === CARDANO_MAINNET_LEDGER;

const getCardanoExplorerUrl = (txId, ledger) => {
  const baseUrl = isMainnet(ledger)
    ? 'https://explorer.cardano.org/en/transaction?id='
    : 'https://explorer.cardano-testnet.iohkdev.io/en/transaction?id=';
  return baseUrl.concat(txId);
};

const SPACING = 2;

const CredentialSummaryDetail = ({ drawerInfo, credential }) => {
  const { t } = useTranslation();
  const [rawVisible, setRawVisible] = useState(false);

  const {
    encodedSignedCredential,
    proof,
    contactData: { contactName },
    credentialData: { credentialTypeDetails },
    verificationResult: {
      credentialSigned,
      credentialPublished,
      credentialRevoked,
      batchRevoked,
      invalidMerkleProof,
      invalidKey,
      keyRevoked,
      invalidSignature
    } = {}
  } = credential;

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

  const credentialToDownload = JSON.stringify({ encodedSignedCredential, proof }, null, SPACING);

  const downloadHref = `data:application/json;charset=utf-8,${encodeURIComponent(
    credentialToDownload
  )}`;

  const downloadName = `${contactName}-${credentialTypeDetails?.name}.json`.replace(' ', '_');

  const disableDownload = !encodedSignedCredential;

  const downloadProps = {
    downloadHref,
    downloadName,
    disabled: disableDownload,
    helpText: disableDownload ? t('credentials.drawer.raw.disabledDownloadHelp') : ''
  };

  const isCredentialReceived = !credential.credentialString;

  const content = (
    <div>
      <DownloadRawButton {...downloadProps} />
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          disabled: !isCredentialReceived,
          onClick: () => setRawVisible(true)
        }}
        buttonText="View"
      />
    </div>
  );

  const getCredentialStatusBadgeData = () => {
    if (credentialRevoked || batchRevoked) return ['revoked', invalidIcon, 'InvalidButton'];
    if (invalidMerkleProof || invalidKey || keyRevoked || invalidSignature) {
      return ['invalid', invalidIcon, 'InvalidButton'];
    }
    if (!credentialSigned) return ['draft', pendingIcon, 'PendingButton'];
    if (!credentialPublished) return ['pendingPublication', pendingIcon, 'PendingButton'];
    return ['valid', verifiedIcon, 'ValidButton'];
  };

  const [credentialStatus, validityIcon, badgeClassName] = getCredentialStatusBadgeData();

  const credentialStatusBadge = (
    <div className={badgeClassName}>
      <img src={validityIcon} alt="verifiedIcon" />
      <p>{t(`credentials.detail.${credentialStatus}`)}</p>
    </div>
  );

  const infoFirstCard = (
    <>
      <div className="credentialStatusContent">
        <span>{t('credentials.detail.source')}</span>
        <p>{credential?.issuer}</p>
      </div>
      <div className="credentialStatusContent">
        <span>{t('credentials.detail.integrity.title')}</span>
        <p>{t(`credentials.detail.integrity.${invalidSignature ? 'invalid' : 'valid'}`)}</p>
      </div>
    </>
  );

  const renderHtmlCredential = ({ credentialData }) => {
    const cleanHtml = sanitizeView(credentialData?.html);
    /* eslint-disable-next-line react/no-danger */
    return <div dangerouslySetInnerHTML={{ __html: cleanHtml }} />;
  };

  const cardanoExplorerHeader = (
    <div className="CardanoExplorerHeader">
      <img className="CardanoLogo" src={cardanoLogo} alt="CardanoLogo" />
      <span>{t('credentials.detail.viewCardanoExplorer')}</span>
    </div>
  );

  const { transactionid, ledger } = credential?.issuanceProof || {};

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
          <div className="credentialContainer">{renderHtmlCredential({ ...credential })}</div>
          {credential.status === CREDENTIAL_STATUSES.credentialRevoked && (
            <div className="revokedAlertContainer">
              <Divider />
              <Alert message={revokedMessage} type="error" showIcon icon={revokedIcon} />
            </div>
          )}
        </TabPane>
        <TabPane tab={tabs.details.title} key={tabs.details.key}>
          <DetailCard
            title={t('credentials.detail.statusTitle')}
            info={infoFirstCard}
            badge={credentialStatusBadge}
          >
            <DataDetail
              img={hashedFile}
              title={t('credentials.detail.hashedFile')}
              data={t('credentials.detail.rawCredential')}
              contentPopOver={content}
            />
          </DetailCard>
          <DetailCard title={cardanoExplorerHeader}>
            <CustomButton
              overrideClassName="GoToCardanoBtn theme-primary"
              buttonProps={{
                target: '_blank',
                href: getCardanoExplorerUrl(transactionid, ledger),
                disabled: !transactionid
              }}
              buttonText={t('credentials.detail.goToCardano')}
            />
          </DetailCard>
        </TabPane>
      </Tabs>
      {!isCredentialReceived && (
        <CredentialRawDetail
          visible={rawVisible}
          credentialString={credential.credentialString}
          downloadProps={downloadProps}
          onClose={() => setRawVisible(false)}
        />
      )}
    </Drawer>
  );
};

CredentialSummaryDetail.propTypes = {
  drawerInfo: PropTypes.shape().isRequired,
  credential: PropTypes.oneOfType([credentialShape, credentialReceivedShape]).isRequired
};

export default CredentialSummaryDetail;
