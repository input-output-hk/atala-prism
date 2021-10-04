import governmentIdLogo from '../../../images/government-id-logo.svg';
import educationalLogo from '../../../images/educational-credential-logo.svg';
import proofOfEmploymentLogo from '../../../images/proof-of-employment-logo.svg';
import healthInsuranceLogo from '../../../images/health-insurance-logo.svg';
import governmentIdSample from '../../../images/government-id-sample.svg';
import educationalSample from '../../../images/educational-credential-sample.svg';
import proofOfEmploymentSample from '../../../images/proof-of-employment-sample.svg';
import healthInsuranceSample from '../../../images/health-insurance-sample.svg';

const governmentId = {
  id: '1',
  name: 'Government ID',
  logo: governmentIdLogo,
  sampleImage: governmentIdSample,
  state: 1
};
const educational = {
  id: '2',
  name: 'Educational',
  logo: educationalLogo,
  sampleImage: educationalSample,
  state: 1
};
const proofOfEmployment = {
  id: '3',
  name: 'Proof Of Employment',
  logo: proofOfEmploymentLogo,
  sampleImage: proofOfEmploymentSample,
  state: 1
};
const healthIsurance = {
  id: '4',
  name: 'Health Insurance',
  logo: healthInsuranceLogo,
  sampleImage: healthInsuranceSample,
  state: 0
};
const disabledCategory = {
  id: '5',
  name: 'Disabled Category',
  state: 0
};

export default [governmentId, educational, proofOfEmployment, healthIsurance, disabledCategory];
