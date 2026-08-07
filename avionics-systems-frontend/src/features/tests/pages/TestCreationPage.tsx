import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import combinedApi, { CreateTestRequest, TestStep } from '../../../api/testApi';
import { projectApi } from '../../../api/projectApi';
import { searchApi } from '../../../api/serviceApi';
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Plus,
  Trash2,
  GripVertical,
  Search,
  Tag,
  FileText,
  Link,
  Settings,
  Save,
  Loader2,
  AlertCircle,
  ChevronDown,
  X,
  RefreshCw,
} from 'lucide-react';

// Wizard Steps
type WizardStep = 'details' | 'steps' | 'requirements' | 'labels' | 'settings' | 'review';

interface StepConfig {
  id: WizardStep;
  title: string;
  icon: React.ReactNode;
  description: string;
}

const WIZARD_STEPS: StepConfig[] = [
  { id: 'details', title: 'Test Details', icon: <FileText className="w-4 h-4" />, description: 'Name, type, and description' },
  { id: 'steps', title: 'Test Steps', icon: <Settings className="w-4 h-4" />, description: 'Define test steps and expected results' },
  { id: 'requirements', title: 'Requirements', icon: <Link className="w-4 h-4" />, description: 'Link requirements for traceability' },
  { id: 'labels', title: 'Labels', icon: <Tag className="w-4 h-4" />, description: 'Add labels for organization' },
  { id: 'settings', title: 'Settings', icon: <Settings className="w-4 h-4" />, description: 'Priority, owner, and other settings' },
  { id: 'review', title: 'Review', icon: <Check className="w-4 h-4" />, description: 'Review and create test' },
];

// Fallback test types (used when API is unavailable)
const FALLBACK_TEST_TYPES = [
  { value: 'MANUAL', label: 'Manual Test', description: 'Manual step-by-step execution' },
  { value: 'AUTOMATED', label: 'Automated Test', description: 'CI/CD integrated automated test' },
  { value: 'BDD', label: 'BDD / Cucumber', description: 'Behavior-driven development scenario' },
];

// Priority levels
const PRIORITIES = [
  { value: 'CRITICAL', label: 'Critical', color: 'bg-red-100 text-red-800' },
  { value: 'HIGH', label: 'High', color: 'bg-orange-100 text-orange-800' },
  { value: 'MEDIUM', label: 'Medium', color: 'bg-yellow-100 text-yellow-800' },
  { value: 'LOW', label: 'Low', color: 'bg-green-100 text-green-800' },
];

interface TestFormData {
  projectId: string;
  name: string;
  description: string;
  testType: 'MANUAL' | 'AUTOMATED' | 'BDD';
  priority: string;
  precondition: string;
  labels: string[];
  requirementKeys: string[];
  testSteps: TestStep[];
  gherkinFeatureKey?: string;
  gherkinScenarioId?: string;
}

const initialFormData: TestFormData = {
  projectId: '',
  name: '',
  description: '',
  testType: 'MANUAL',
  priority: 'MEDIUM',
  precondition: '',
  labels: [],
  requirementKeys: [],
  testSteps: [{ index: 1, description: '', expectedResult: '', testData: '' }],
};

export const TestCreationPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { projectId } = useParams<{ projectId: string }>();

  const [currentStep, setCurrentStep] = useState<WizardStep>('details');
  const [formData, setFormData] = useState<TestFormData>({
    ...initialFormData,
    projectId: projectId || '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [newLabel, setNewLabel] = useState('');
  const [newRequirement, setNewRequirement] = useState('');
  const [searchRequirements, setSearchRequirements] = useState('');
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  // Fetch dynamic test types from admin config (fallback to hardcoded)
  const { data: dynamicTestTypes } = useQuery({
    queryKey: ['test-admin', 'test-types'],
    queryFn: () => combinedApi.getTestTypes(),
    staleTime: 60000,
  });
  const TEST_TYPES = dynamicTestTypes?.length
    ? dynamicTestTypes.filter(t => t.isActive).map(t => ({
        value: t.name,
        label: t.displayName,
        description: t.description || t.displayName,
      }))
    : FALLBACK_TEST_TYPES;

  // Fetch projects from API
  const { data: availableProjects = [], isLoading: projectsLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectApi.getAll({ archived: false }),
  });

  // Fetch existing requirements for search - using issue search API
  const { data: requirements = [], isLoading: requirementsLoading } = useQuery({
    queryKey: ['requirements', formData.projectId, searchRequirements],
    queryFn: async () => {
      // Use the search API to find requirement-like issues
      try {
        const response = await searchApi.search({
          query: searchRequirements || '',
          projectId: formData.projectId,
          entityType: 'requirement',
          size: 20,
        });

        if (response.data?.results && response.data.results.length > 0) {
          return response.data.results.map((r) => ({
            key: r.entityId.replace('REQ-', '').replace(/-/g, '').toUpperCase() || r.entityId,
            title: r.title,
          }));
        }
      } catch (error) {
        console.error('Failed to search requirements:', error);
      }

      // Fallback: try to get issues that might be requirements using the search API
      try {
        const jqlResponse = await searchApi.jqlSearch({
          jql: `project = ${formData.projectId} AND issueType = Requirement ORDER BY key`,
          size: 20,
        });

        if (jqlResponse.data?.results && jqlResponse.data.results.length > 0) {
          return jqlResponse.data.results.map((r) => ({
            key: r.entityId,
            title: r.title,
          }));
        }
      } catch (error) {
        console.error('Failed JQL search for requirements:', error);
      }

      // Return empty array if no requirements found
      return [];
    },
    enabled: !!formData.projectId,
  });

  const filteredRequirements = requirements.filter(
    (req: { key: string; title: string }) =>
      req.key.toLowerCase().includes(searchRequirements.toLowerCase()) ||
      req.title.toLowerCase().includes(searchRequirements.toLowerCase())
  );

  // Create test mutation
  const createTestMutation = useMutation({
    mutationFn: (data: CreateTestRequest) => combinedApi.createTest(data),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['tests'] });
      navigate(`/tests/${response.id}`);
    },
    onError: (error) => {
      setErrors({ submit: 'Failed to create test. Please try again.' });
    },
  });

  const updateField = (field: keyof TestFormData, value: unknown) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  const addTestStep = () => {
    const newStep: TestStep = {
      index: formData.testSteps.length + 1,
      description: '',
      expectedResult: '',
      testData: '',
    };
    updateField('testSteps', [...formData.testSteps, newStep]);
  };

  const updateTestStep = (index: number, field: keyof TestStep, value: string) => {
    const updated = formData.testSteps.map((step, i) =>
      i === index ? { ...step, [field]: value } : step
    );
    updateField('testSteps', updated);
  };

  const removeTestStep = (index: number) => {
    const updated = formData.testSteps
      .filter((_, i) => i !== index)
      .map((step, i) => ({ ...step, index: i + 1 }));
    updateField('testSteps', updated);
  };

  const addLabel = () => {
    if (newLabel.trim() && !formData.labels.includes(newLabel.trim())) {
      updateField('labels', [...formData.labels, newLabel.trim()]);
      setNewLabel('');
    }
  };

  const removeLabel = (label: string) => {
    updateField('labels', formData.labels.filter((l) => l !== label));
  };

  const addRequirement = (key: string) => {
    if (!formData.requirementKeys.includes(key)) {
      updateField('requirementKeys', [...formData.requirementKeys, key]);
    }
    setNewRequirement('');
    setIsDropdownOpen(false);
  };

  const removeRequirement = (key: string) => {
    updateField('requirementKeys', formData.requirementKeys.filter((k) => k !== key));
  };

  const validateStep = (step: WizardStep): boolean => {
    const newErrors: Record<string, string> = {};

    switch (step) {
      case 'details':
        if (!formData.name.trim()) newErrors.name = 'Test name is required';
        if (!formData.projectId) newErrors.projectId = 'Project is required';
        break;
      case 'steps':
        const hasValidStep = formData.testSteps.some(
          (s) => s.description.trim() && s.expectedResult.trim()
        );
        if (!hasValidStep) newErrors.testSteps = 'At least one complete step is required';
        break;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const goToNext = () => {
    if (!validateStep(currentStep)) return;

    const currentIndex = WIZARD_STEPS.findIndex((s) => s.id === currentStep);
    if (currentIndex < WIZARD_STEPS.length - 1) {
      setCurrentStep(WIZARD_STEPS[currentIndex + 1].id);
    }
  };

  const goToPrevious = () => {
    const currentIndex = WIZARD_STEPS.findIndex((s) => s.id === currentStep);
    if (currentIndex > 0) {
      setCurrentStep(WIZARD_STEPS[currentIndex - 1].id);
    }
  };

  const handleSubmit = () => {
    if (!validateStep(currentStep)) return;

    const { testSteps, ...rest } = formData;
    const cleanData = {
      ...rest,
      steps: testSteps
        .filter((s) => s.description.trim() || s.expectedResult.trim())
        .map((s) => ({
          stepOrder: s.index,
          description: s.description,
          expectedResult: s.expectedResult,
          testData: s.testData,
        })),
    };

    createTestMutation.mutate(cleanData);
  };

  const currentStepIndex = WIZARD_STEPS.findIndex((s) => s.id === currentStep);
  const isLastStep = currentStepIndex === WIZARD_STEPS.length - 1;
  const isFirstStep = currentStepIndex === 0;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-5xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => navigate(-1)}
                className="p-2 hover:bg-gray-100 rounded-lg"
              >
                <ArrowLeft className="w-5 h-5 text-gray-600" />
              </button>
              <div>
                <h1 className="text-xl font-semibold text-gray-900">Create Test</h1>
                <p className="text-sm text-gray-500">Step {currentStepIndex + 1} of {WIZARD_STEPS.length}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-5xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            {WIZARD_STEPS.map((step, index) => (
              <React.Fragment key={step.id}>
                <div
                  className={`flex flex-col items-center ${
                    index <= currentStepIndex ? 'text-blue-600' : 'text-gray-400'
                  }`}
                >
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center ${
                      index < currentStepIndex
                        ? 'bg-blue-600 text-white'
                        : index === currentStepIndex
                        ? 'bg-blue-100 border-2 border-blue-600'
                        : 'bg-gray-100'
                    }`}
                  >
                    {index < currentStepIndex ? (
                      <Check className="w-4 h-4" />
                    ) : (
                      <span className="text-sm font-medium">{index + 1}</span>
                    )}
                  </div>
                  <span className="text-xs mt-1 hidden md:block">{step.title}</span>
                </div>
                {index < WIZARD_STEPS.length - 1 && (
                  <div
                    className={`flex-1 h-0.5 mx-2 ${
                      index < currentStepIndex ? 'bg-blue-600' : 'bg-gray-200'
                    }`}
                  />
                )}
              </React.Fragment>
            ))}
          </div>
        </div>
      </div>

      {/* Content Area */}
      <div className="max-w-5xl mx-auto px-6 py-8">
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          {/* Step Header */}
          <div className="mb-6">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 bg-blue-100 rounded-lg text-blue-600">
                {WIZARD_STEPS[currentStepIndex].icon}
              </div>
              <div>
                <h2 className="text-lg font-semibold text-gray-900">
                  {WIZARD_STEPS[currentStepIndex].title}
                </h2>
                <p className="text-sm text-gray-500">
                  {WIZARD_STEPS[currentStepIndex].description}
                </p>
              </div>
            </div>
          </div>

          {/* Step Content */}
          <div className="min-h-[400px]">
            {currentStep === 'details' && (
              <DetailsStep
                formData={formData}
                errors={errors}
                updateField={updateField}
                availableProjects={availableProjects}
                projectsLoading={projectsLoading}
              />
            )}
            {currentStep === 'steps' && (
              <StepsStep
                steps={formData.testSteps}
                errors={errors}
                updateStep={updateTestStep}
                addStep={addTestStep}
                removeStep={removeTestStep}
              />
            )}
            {currentStep === 'requirements' && (
              <RequirementsStep
                requirements={formData.requirementKeys}
                availableRequirements={filteredRequirements}
                searchQuery={searchRequirements}
                setSearchQuery={setSearchRequirements}
                isDropdownOpen={isDropdownOpen}
                setIsDropdownOpen={setIsDropdownOpen}
                addRequirement={addRequirement}
                removeRequirement={removeRequirement}
              />
            )}
            {currentStep === 'labels' && (
              <LabelsStep
                labels={formData.labels}
                newLabel={newLabel}
                setNewLabel={setNewLabel}
                addLabel={addLabel}
                removeLabel={removeLabel}
              />
            )}
            {currentStep === 'settings' && (
              <SettingsStep formData={formData} updateField={updateField} />
            )}
            {currentStep === 'review' && (
              <ReviewStep formData={formData} />
            )}
          </div>

          {/* Error Message */}
          {errors.submit && (
            <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3">
              <AlertCircle className="w-5 h-5 text-red-600" />
              <span className="text-red-700">{errors.submit}</span>
            </div>
          )}

          {/* Navigation Buttons */}
          <div className="mt-8 pt-6 border-t border-gray-200 flex items-center justify-between">
            <button
              onClick={goToPrevious}
              disabled={isFirstStep}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg ${
                isFirstStep
                  ? 'text-gray-300 cursor-not-allowed'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <ArrowLeft className="w-4 h-4" />
              Previous
            </button>

            {isLastStep ? (
              <button
                onClick={handleSubmit}
                disabled={createTestMutation.isPending}
                className="flex items-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {createTestMutation.isPending ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Save className="w-4 h-4" />
                )}
                Create Test
              </button>
            ) : (
              <button
                onClick={goToNext}
                className="flex items-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                Next
                <ArrowRight className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

// Step Components
interface DetailsStepProps {
  formData: TestFormData;
  errors: Record<string, string>;
  updateField: (field: keyof TestFormData, value: unknown) => void;
  availableProjects: { id: string; name: string; projectKey?: string }[];
  projectsLoading?: boolean;
}

const DetailsStep: React.FC<DetailsStepProps> = ({ formData, errors, updateField, availableProjects, projectsLoading }) => (
  <div className="space-y-6">
    {/* Project */}
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">
        Project <span className="text-red-500">*</span>
      </label>
      <select
        value={formData.projectId}
        onChange={(e) => updateField('projectId', e.target.value)}
        disabled={projectsLoading}
        className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
          errors.projectId ? 'border-red-500' : 'border-gray-300'
        } ${projectsLoading ? 'bg-gray-100' : ''}`}
      >
        <option value="">
          {projectsLoading ? 'Loading projects...' : 'Select a project'}
        </option>
        {availableProjects.map((proj) => (
          <option key={proj.id} value={proj.id}>
            {proj.projectKey ? `${proj.projectKey} - ${proj.name}` : proj.name}
          </option>
        ))}
      </select>
      {errors.projectId && (
        <p className="mt-1 text-sm text-red-500">{errors.projectId}</p>
      )}
    </div>

    {/* Test Name */}
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">
        Test Name <span className="text-red-500">*</span>
      </label>
      <input
        type="text"
        value={formData.name}
        onChange={(e) => updateField('name', e.target.value)}
        placeholder="Enter test name"
        className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
          errors.name ? 'border-red-500' : 'border-gray-300'
        }`}
      />
      {errors.name && <p className="mt-1 text-sm text-red-500">{errors.name}</p>}
    </div>

    {/* Test Type */}
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-2">Test Type</label>
      <div className="grid grid-cols-3 gap-4">
        {TEST_TYPES.map((type) => (
          <button
            key={type.value}
            onClick={() => updateField('testType', type.value)}
            className={`p-4 border rounded-lg text-left transition-all ${
              formData.testType === type.value
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-200 hover:border-gray-300'
            }`}
          >
            <div className="font-medium text-gray-900">{type.label}</div>
            <div className="text-sm text-gray-500 mt-1">{type.description}</div>
          </button>
        ))}
      </div>
    </div>

    {/* Description */}
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
      <textarea
        value={formData.description}
        onChange={(e) => updateField('description', e.target.value)}
        placeholder="Describe the test scenario..."
        rows={4}
        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
    </div>

    {/* Precondition */}
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">Precondition</label>
      <textarea
        value={formData.precondition}
        onChange={(e) => updateField('precondition', e.target.value)}
        placeholder="Enter any prerequisites or setup steps..."
        rows={3}
        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
    </div>
  </div>
);

const StepsStep: React.FC<{
  steps: TestStep[];
  errors: Record<string, string>;
  updateStep: (index: number, field: keyof TestStep, value: string) => void;
  addStep: () => void;
  removeStep: (index: number) => void;
}> = ({ steps, errors, updateStep, addStep, removeStep }) => (
  <div className="space-y-4">
    {errors.testSteps && (
      <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-red-700">
        <AlertCircle className="w-4 h-4" />
        {errors.testSteps}
      </div>
    )}

    <div className="space-y-3">
      {steps.map((step, index) => (
        <div key={index} className="border border-gray-200 rounded-lg p-4 bg-gray-50">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <GripVertical className="w-4 h-4 text-gray-400 cursor-move" />
              <span className="font-medium text-gray-700">Step {step.index}</span>
            </div>
            {steps.length > 1 && (
              <button
                onClick={() => removeStep(index)}
                className="p-1 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">
                Action / Step Description
              </label>
              <textarea
                value={step.description}
                onChange={(e) => updateStep(index, 'description', e.target.value)}
                placeholder="Enter the step action..."
                rows={2}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">
                Expected Result
              </label>
              <textarea
                value={step.expectedResult}
                onChange={(e) => updateStep(index, 'expectedResult', e.target.value)}
                placeholder="Enter expected outcome..."
                rows={2}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div className="mt-3">
            <label className="block text-sm font-medium text-gray-600 mb-1">
              Test Data (Optional)
            </label>
            <input
              type="text"
              value={step.testData || ''}
              onChange={(e) => updateStep(index, 'testData', e.target.value)}
              placeholder="Enter test data for this step..."
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>
      ))}
    </div>

    <button
      onClick={addStep}
      className="flex items-center gap-2 px-4 py-2 border border-dashed border-gray-300 rounded-lg text-gray-600 hover:border-blue-500 hover:text-blue-600"
    >
      <Plus className="w-4 h-4" />
      Add Step
    </button>
  </div>
);

const RequirementsStep: React.FC<{
  requirements: string[];
  availableRequirements: { key: string; title: string }[];
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  isDropdownOpen: boolean;
  setIsDropdownOpen: (open: boolean) => void;
  addRequirement: (key: string) => void;
  removeRequirement: (key: string) => void;
}> = ({
  requirements,
  availableRequirements,
  searchQuery,
  setSearchQuery,
  isDropdownOpen,
  setIsDropdownOpen,
  addRequirement,
  removeRequirement,
}) => (
  <div className="space-y-4">
    <p className="text-sm text-gray-500">
      Link requirements to establish traceability between tests and requirements.
    </p>

    {/* Search/Add Input */}
    <div className="relative">
      <div className="flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setIsDropdownOpen(true);
            }}
            onFocus={() => setIsDropdownOpen(true)}
            placeholder="Search requirements (e.g., REQ-001)"
            className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      {/* Dropdown */}
      {isDropdownOpen && (
        <div className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-auto">
          {availableRequirements.length > 0 ? (
            availableRequirements.map((req) => (
              <button
                key={req.key}
                onClick={() => addRequirement(req.key)}
                className="w-full px-4 py-2 text-left hover:bg-gray-50 flex items-center justify-between"
              >
                <span className="font-mono text-sm text-blue-600">{req.key}</span>
                <span className="text-sm text-gray-600">{req.title}</span>
              </button>
            ))
          ) : (
            <div className="px-4 py-3 text-sm text-gray-500">
              No matching requirements found
            </div>
          )}
        </div>
      )}
    </div>

    {/* Selected Requirements */}
    {requirements.length > 0 && (
      <div className="space-y-2">
        <h4 className="text-sm font-medium text-gray-700">Linked Requirements</h4>
        <div className="flex flex-wrap gap-2">
          {requirements.map((key) => (
            <span
              key={key}
              className="inline-flex items-center gap-2 px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm"
            >
              <Link className="w-3 h-3" />
              {key}
              <button
                onClick={() => removeRequirement(key)}
                className="ml-1 hover:bg-blue-200 rounded-full p-0.5"
              >
                <X className="w-3 h-3" />
              </button>
            </span>
          ))}
        </div>
      </div>
    )}

    {requirements.length === 0 && (
      <div className="text-center py-8 text-gray-500">
        <Link className="w-12 h-12 mx-auto mb-3 text-gray-300" />
        <p>No requirements linked yet</p>
        <p className="text-sm">Search and select requirements above to link them</p>
      </div>
    )}
  </div>
);

const LabelsStep: React.FC<{
  labels: string[];
  newLabel: string;
  setNewLabel: (label: string) => void;
  addLabel: () => void;
  removeLabel: (label: string) => void;
}> = ({ labels, newLabel, setNewLabel, addLabel, removeLabel }) => (
  <div className="space-y-4">
    <p className="text-sm text-gray-500">
      Add labels to categorize and organize your tests for easier filtering and search.
    </p>

    {/* Add Label Input */}
    <div className="flex gap-2">
      <input
        type="text"
        value={newLabel}
        onChange={(e) => setNewLabel(e.target.value)}
        onKeyPress={(e) => e.key === 'Enter' && addLabel()}
        placeholder="Enter label name..."
        className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
      <button
        onClick={addLabel}
        disabled={!newLabel.trim()}
        className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
      >
        Add Label
      </button>
    </div>

    {/* Labels */}
    {labels.length > 0 ? (
      <div className="flex flex-wrap gap-2">
        {labels.map((label) => (
          <span
            key={label}
            className="inline-flex items-center gap-2 px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm"
          >
            <Tag className="w-3 h-3" />
            {label}
            <button
              onClick={() => removeLabel(label)}
              className="ml-1 hover:bg-gray-200 rounded-full p-0.5"
            >
              <X className="w-3 h-3" />
            </button>
          </span>
        ))}
      </div>
    ) : (
      <div className="text-center py-8 text-gray-500">
        <Tag className="w-12 h-12 mx-auto mb-3 text-gray-300" />
        <p>No labels added yet</p>
        <p className="text-sm">Type a label name and press Enter or click Add Label</p>
      </div>
    )}

    {/* Suggested Labels */}
    <div>
      <h4 className="text-sm font-medium text-gray-700 mb-2">Suggested Labels</h4>
      <div className="flex flex-wrap gap-2">
        {['regression', 'smoke', 'integration', 'e2e', 'api'].map((suggested) => (
          <button
            key={suggested}
            onClick={() => {
              if (!labels.includes(suggested)) {
                setNewLabel(suggested);
              }
            }}
            className="px-3 py-1 bg-gray-50 border border-dashed border-gray-300 rounded-full text-sm text-gray-600 hover:border-blue-500 hover:text-blue-600"
          >
            + {suggested}
          </button>
        ))}
      </div>
    </div>
  </div>
);

const SettingsStep: React.FC<{
  formData: TestFormData;
  updateField: (field: keyof TestFormData, value: unknown) => void;
}> = ({ formData, updateField }) => (
  <div className="space-y-6">
    {/* Priority */}
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-2">Priority</label>
      <div className="flex gap-3">
        {PRIORITIES.map((priority) => (
          <button
            key={priority.value}
            onClick={() => updateField('priority', priority.value)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
              formData.priority === priority.value
                ? `${priority.color} border-2 border-gray-400`
                : 'bg-gray-50 text-gray-600 hover:bg-gray-100'
            }`}
          >
            {priority.label}
          </button>
        ))}
      </div>
    </div>

    {/* BDD Settings (if type is BDD) */}
    {formData.testType === 'BDD' && (
      <div className="space-y-4 p-4 bg-green-50 rounded-lg border border-green-200">
        <h4 className="font-medium text-green-800">BDD Settings</h4>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Feature File Key
          </label>
          <input
            type="text"
            value={formData.gherkinFeatureKey || ''}
            onChange={(e) => updateField('gherkinFeatureKey', e.target.value)}
            placeholder="e.g., FEATURE-123"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Scenario ID
          </label>
          <input
            type="text"
            value={formData.gherkinScenarioId || ''}
            onChange={(e) => updateField('gherkinScenarioId', e.target.value)}
            placeholder="e.g., scenario-1"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>
    )}

    {/* Summary */}
    <div className="p-4 bg-gray-50 rounded-lg">
      <h4 className="font-medium text-gray-700 mb-3">Summary</h4>
      <dl className="grid grid-cols-2 gap-3 text-sm">
        <div>
          <dt className="text-gray-500">Test Type</dt>
          <dd className="font-medium">{formData.testType}</dd>
        </div>
        <div>
          <dt className="text-gray-500">Priority</dt>
          <dd className="font-medium">{formData.priority}</dd>
        </div>
        <div>
          <dt className="text-gray-500">Labels</dt>
          <dd className="font-medium">{formData.labels.length || 0}</dd>
        </div>
        <div>
          <dt className="text-gray-500">Requirements</dt>
          <dd className="font-medium">{formData.requirementKeys.length || 0}</dd>
        </div>
        <div>
          <dt className="text-gray-500">Steps</dt>
          <dd className="font-medium">{formData.testSteps.length}</dd>
        </div>
      </dl>
    </div>
  </div>
);

const ReviewStep: React.FC<{ formData: TestFormData }> = ({ formData }) => (
  <div className="space-y-6">
    <div className="p-4 bg-blue-50 rounded-lg flex items-center gap-3">
      <Check className="w-5 h-5 text-blue-600" />
      <span className="text-blue-700">Review your test configuration before creating</span>
    </div>

    {/* Test Details */}
    <div className="border border-gray-200 rounded-lg p-4">
      <h4 className="font-medium text-gray-900 mb-3">Test Details</h4>
      <dl className="space-y-2">
        <div className="flex">
          <dt className="w-32 text-gray-500">Name:</dt>
          <dd className="font-medium">{formData.name || 'Not specified'}</dd>
        </div>
        <div className="flex">
          <dt className="w-32 text-gray-500">Type:</dt>
          <dd className="font-medium">{formData.testType}</dd>
        </div>
        <div className="flex">
          <dt className="w-32 text-gray-500">Priority:</dt>
          <dd className="font-medium">{formData.priority}</dd>
        </div>
        {formData.description && (
          <div className="flex">
            <dt className="w-32 text-gray-500">Description:</dt>
            <dd className="text-gray-600">{formData.description}</dd>
          </div>
        )}
        {formData.precondition && (
          <div className="flex">
            <dt className="w-32 text-gray-500">Precondition:</dt>
            <dd className="text-gray-600">{formData.precondition}</dd>
          </div>
        )}
      </dl>
    </div>

    {/* Test Steps */}
    <div className="border border-gray-200 rounded-lg p-4">
      <h4 className="font-medium text-gray-900 mb-3">
        Test Steps ({formData.testSteps.filter(s => s.description).length})
      </h4>
      <div className="space-y-2">
        {formData.testSteps.filter(s => s.description).map((step, index) => (
          <div key={index} className="p-3 bg-gray-50 rounded-lg">
            <div className="font-medium text-sm text-gray-700">Step {index + 1}</div>
            <div className="text-sm text-gray-600 mt-1">{step.description}</div>
            <div className="text-sm text-gray-500 mt-1">
              <span className="font-medium">Expected:</span> {step.expectedResult}
            </div>
          </div>
        ))}
      </div>
    </div>

    {/* Requirements & Labels */}
    <div className="grid grid-cols-2 gap-4">
      <div className="border border-gray-200 rounded-lg p-4">
        <h4 className="font-medium text-gray-900 mb-3">
          Requirements ({formData.requirementKeys.length})
        </h4>
        {formData.requirementKeys.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {formData.requirementKeys.map((key) => (
              <span key={key} className="px-2 py-1 bg-blue-100 text-blue-800 rounded text-sm">
                {key}
              </span>
            ))}
          </div>
        ) : (
          <p className="text-sm text-gray-500">No requirements linked</p>
        )}
      </div>

      <div className="border border-gray-200 rounded-lg p-4">
        <h4 className="font-medium text-gray-900 mb-3">Labels ({formData.labels.length})</h4>
        {formData.labels.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {formData.labels.map((label) => (
              <span key={label} className="px-2 py-1 bg-gray-100 text-gray-700 rounded text-sm">
                {label}
              </span>
            ))}
          </div>
        ) : (
          <p className="text-sm text-gray-500">No labels added</p>
        )}
      </div>
    </div>
  </div>
);

export default TestCreationPage;